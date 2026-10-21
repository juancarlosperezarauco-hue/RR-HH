package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.engine;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.config.BackupProperties;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupExecutionException;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupArtifact;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupScope;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.ChecksumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PgBackupEngineAdapter implements BackupEngine {

    private static final Logger log = LoggerFactory.getLogger(PgBackupEngineAdapter.class);
    private static final int OUTPUT_LIMIT = 8 * 1024 * 1024;
    private static final Duration COMMAND_TIMEOUT = Duration.ofHours(2);
    private static final Pattern TOC_ENTRY = Pattern.compile("^\\s*\\d+;\\s+\\d+\\s+\\d+\\s+(.+)$");
    private static final Pattern SCHEMA_DECLARATION = Pattern.compile("^SCHEMA\\s+-\\s+(\\S+)\\s+.*$");
    private static final Pattern NAMESPACED_OBJECT = Pattern.compile("^(?:TABLE DATA|MATERIALIZED VIEW DATA|SEQUENCE SET|SEQUENCE OWNED BY|FK CONSTRAINT|CHECK CONSTRAINT|TABLE|SEQUENCE|VIEW|MATERIALIZED VIEW|FUNCTION|PROCEDURE|TRIGGER|INDEX|CONSTRAINT|DEFAULT|TYPE|DOMAIN|AGGREGATE|RULE|POLICY|FOREIGN TABLE|COMMENT|ACL)\\s+(\\S+)\\s+.+$");
    private static final Pattern UNSAFE_TENANT_OBJECT = Pattern.compile("^(?:DATABASE|EXTENSION|EVENT TRIGGER|PUBLICATION|SUBSCRIPTION|SERVER|USER MAPPING|FOREIGN DATA WRAPPER|BLOB|BLOB DATA)\\s+.*$");
    private static final Pattern PUBLIC_TENANTS_TABLE = Pattern.compile("^TABLE\\s+public\\s+platform_tenants\\s+.*$");

    private final BackupProperties properties;
    private final ChecksumService checksumService;

    public PgBackupEngineAdapter(BackupProperties properties, ChecksumService checksumService) {
        this.properties = properties;
        this.checksumService = checksumService;
    }

    @Override
    public BackupArtifact dumpSchema(String schemaName, Path target) {
        validateSqlIdentifier(schemaName, "schemaName");
        runStrict(
                List.of(
                        properties.getPgDumpPath(),
                        "-h", properties.getHost(),
                        "-p", properties.getPort(),
                        "-U", properties.getUsername(),
                        "-d", properties.getDatabase(),
                        "-n", schemaName,
                        "-Fc",
                        "-f", target.toString()
                ),
                "pg_dump schema " + schemaName
        );

        return artifact(target);
    }

    @Override
    public BackupArtifact dumpFullDatabase(Path target) {
        runStrict(
                buildDumpFullDatabaseCommand(target),
                "pg_dump full database"
        );

        return artifact(target);
    }

    @Override
    public void validateArchive(Path source, BackupScope scope, String schemaName) {
        if (scope == null) throw new BackupExecutionException("Debe especificar el alcance del respaldo");
        if (source == null || !Files.isRegularFile(source) || !Files.isReadable(source)) throw new BackupExecutionException("El archivo de respaldo no existe o no puede leerse");
        try {
            if (Files.size(source) < 5) throw new BackupExecutionException("El archivo de respaldo está vacío o incompleto");
            try (InputStream input = Files.newInputStream(source)) {
                if (!"PGDMP".equals(new String(input.readNBytes(5), StandardCharsets.US_ASCII))) {
                    throw new BackupExecutionException("El archivo no es un respaldo PostgreSQL custom (-Fc). No se admite SQL, ZIP ni un archivo renombrado como .dump");
                }
            }
        } catch (BackupExecutionException e) { throw e; }
        catch (Exception e) { throw new BackupExecutionException("No se pudo leer la cabecera del respaldo", e); }
        validateToc(runStrict(List.of(properties.getPgRestorePath(), "--list", source.toString()), "inspeccionar catálogo del respaldo"), scope, schemaName);
    }

    private void validateToc(String toc, BackupScope scope, String schemaName) {
        if (scope == BackupScope.TENANT_SCHEMA) {
            validateSqlIdentifier(schemaName, "schemaName");
            boolean foundSchema = false, foundTable = false;
            for (String line : toc.split("\\R")) {
                Matcher entry = TOC_ENTRY.matcher(line); if (!entry.matches()) continue;
                String descriptor = entry.group(1);
                if (UNSAFE_TENANT_OBJECT.matcher(descriptor).matches()) throw new BackupExecutionException("El respaldo contiene un objeto global no permitido");
                Matcher schema = SCHEMA_DECLARATION.matcher(descriptor);
                if (schema.matches()) { if (!schemaName.equals(schema.group(1))) throw new BackupExecutionException("El respaldo pertenece a otro esquema: " + schema.group(1)); foundSchema = true; continue; }
                Matcher object = NAMESPACED_OBJECT.matcher(descriptor);
                if (object.matches()) { String namespace = object.group(1); if (!"-".equals(namespace) && !schemaName.equals(namespace)) throw new BackupExecutionException("El respaldo contiene objetos fuera del tenant esperado: " + namespace); if (descriptor.startsWith("TABLE " + schemaName + " ")) foundTable = true; }
            }
            if (!foundSchema || !foundTable) throw new BackupExecutionException("El archivo no contiene un respaldo completo del esquema " + schemaName);
        } else {
            boolean found = false;
            for (String line : toc.split("\\R")) { Matcher entry = TOC_ENTRY.matcher(line); if (entry.matches() && PUBLIC_TENANTS_TABLE.matcher(entry.group(1)).matches()) { found = true; break; } }
            if (!found) throw new BackupExecutionException("El archivo no parece ser un backup completo de HRCore: falta public.platform_tenants");
        }
    }

    @Override
    public String restoreSchema(String schemaName, Path source) {
        validateArchive(source, BackupScope.TENANT_SCHEMA, schemaName);
        Path sql = null;
        try {
            sql = Files.createTempFile(source.getParent(), "tenant_restore_", ".sql");
            runStrict(List.of(properties.getPgRestorePath(), "--no-owner", "--no-privileges", "--schema=" + schemaName, "--file=" + sql, source.toString()), "generar SQL para tenant " + schemaName);
            runStrict(List.of(properties.getPsqlPath(), "-h", properties.getHost(), "-p", properties.getPort(), "-U", properties.getUsername(), "-d", properties.getDatabase(), "--single-transaction", "-v", "ON_ERROR_STOP=1", "-c", "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE;", "-f", sql.toString()), "restaurar tenant de forma atómica " + schemaName);
            return null;
        } catch (BackupExecutionException e) { throw e; }
        catch (Exception e) { throw new BackupExecutionException("No se pudo preparar el restore del tenant", e); }
        finally { deleteTemporary(sql); }
    }

    @Override
    public String restoreFullDatabase(Path source) {
        validateArchive(source, BackupScope.FULL_DATABASE, null);
        Path tocFile = null;
        try {
            String toc = runStrict(List.of(properties.getPgRestorePath(), "--list", source.toString()), "leer TOC del restore completo");
            StringBuilder filtered = new StringBuilder();
            for (String line : toc.split("\\R", -1)) { Matcher entry = TOC_ENTRY.matcher(line); if (entry.matches() && SCHEMA_DECLARATION.matcher(entry.group(1)).matches() && entry.group(1).matches("SCHEMA\\s+-\\s+public\\s+.*")) filtered.append(';'); filtered.append(line).append(System.lineSeparator()); }
            tocFile = Files.createTempFile(source.getParent(), "full_restore_toc_", ".list"); Files.writeString(tocFile, filtered, StandardCharsets.UTF_8);
            runStrict(List.of(properties.getPgRestorePath(), "-h", properties.getHost(), "-p", properties.getPort(), "-U", properties.getUsername(), "-d", properties.getDatabase(), "--use-list=" + tocFile, "--clean", "--if-exists", "--single-transaction", "--exit-on-error", "--no-owner", "--no-privileges", source.toString()), "pg_restore full database atómico");
            return null;
        } catch (BackupExecutionException e) { throw e; }
        catch (Exception e) { throw new BackupExecutionException("No se pudo preparar el restore completo", e); }
        finally { deleteTemporary(tocFile); }
    }

    List<String> buildDumpFullDatabaseCommand(Path target) {
        return List.of(
                properties.getPgDumpPath(),
                "-h", properties.getHost(),
                "-p", properties.getPort(),
                "-U", properties.getUsername(),
                "-d", properties.getDatabase(),
                "-Fc",
                "--exclude-table=public.backup_jobs",
                "-f", target.toString()
        );
    }

    List<String> buildRestoreFullDatabaseCommand(Path source) {
        return List.of(
                properties.getPgRestorePath(),
                "-h", properties.getHost(),
                "-p", properties.getPort(),
                "-U", properties.getUsername(),
                "-d", properties.getDatabase(),
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-privileges",
                source.toString()
        );
    }

    private void executePsql(String sql) {
        runStrict(
                List.of(
                        properties.getPsqlPath(),
                        "-h", properties.getHost(),
                        "-p", properties.getPort(),
                        "-U", properties.getUsername(),
                        "-d", properties.getDatabase(),
                        "-v", "ON_ERROR_STOP=1",
                        "-c", sql
                ),
                "psql command"
        );
    }

    private BackupArtifact artifact(Path target) {
        try {
            return new BackupArtifact(target, Files.size(target), checksumService.sha256(target));
        } catch (Exception e) {
            throw new BackupExecutionException("Unable to calculate artifact metadata", e);
        }
    }

    private String runStrict(List<String> command, String description) {
        CommandExecution execution = runCommand(command, description);
        if (execution.exitCode() != 0) {
            throw new BackupExecutionException(
                    "Command failed: " + description +
                            ". Exit code: " + execution.exitCode() +
                            ". Output: " + truncate(execution.output())
            );
        }
        return execution.output();
    }

    private String runRestoreTolerant(List<String> command, String description) {
        CommandExecution execution = runCommand(command, description);

        if (execution.exitCode() == 0) {
            return null;
        }

        if (execution.exitCode() == 1) {
            String output = truncate(execution.output());
            log.warn(
                    "Restore command finished with warnings. description={}, exitCode={}, output={}",
                    description,
                    execution.exitCode(),
                    output
            );
            return output;
        }

        throw new BackupExecutionException(
                "Restore command failed: " + description +
                        ". Exit code: " + execution.exitCode() +
                        ". Output: " + truncate(execution.output())
        );
    }

    private CommandExecution runCommand(List<String> command, String description) {
        Process process = null;
        try {
            log.info("Running backup command: {}", description);
            ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(command));
            builder.environment().put("PGPASSWORD", properties.getPassword());
            builder.redirectErrorStream(true);
            process = builder.start();
            final Process running = process;
            final StringBuilder output = new StringBuilder();
            final java.util.concurrent.atomic.AtomicReference<Exception> readerError = new java.util.concurrent.atomic.AtomicReference<>();
            Thread reader = Thread.startVirtualThread(() -> {
                try (BufferedReader input = new BufferedReader(new InputStreamReader(running.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (output.length() + line.length() + 1 > OUTPUT_LIMIT) { readerError.set(new BackupExecutionException("Salida excesiva del comando: " + description)); running.destroyForcibly(); return; }
                        output.append(line).append('\n');
                    }
                } catch (Exception e) { readerError.set(e); }
            });
            if (!process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) { process.destroyForcibly(); throw new BackupExecutionException("Tiempo máximo excedido: " + description); }
            reader.join(TimeUnit.SECONDS.toMillis(10));
            if (reader.isAlive()) { process.destroyForcibly(); throw new BackupExecutionException("No se pudo leer la salida del proceso: " + description); }
            if (readerError.get() != null) throw new BackupExecutionException("Error al leer salida: " + description, readerError.get());
            return new CommandExecution(process.exitValue(), output.toString());
        } catch (InterruptedException e) { if (process != null) process.destroyForcibly(); Thread.currentThread().interrupt(); throw new BackupExecutionException("Comando interrumpido: " + description, e); }
        catch (BackupExecutionException e) { throw e; }
        catch (Exception e) { if (process != null) process.destroyForcibly(); throw new BackupExecutionException("No se pudo ejecutar: " + description, e); }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.length() <= 3500) {
            return trimmed;
        }

        return trimmed.substring(0, 3500) + "... [truncated]";
    }

    private void validateSqlIdentifier(String value, String field) {
        if (value == null || value.isBlank() || !value.matches("^[a-zA-Z0-9_]+$")) {
            throw new BackupExecutionException(field + " is invalid: " + value);
        }
    }

    private void deleteTemporary(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (Exception e) { log.warn("Unable to remove temporary file {}", path, e); }
    }

    private record CommandExecution(int exitCode, String output) {
    }
}
