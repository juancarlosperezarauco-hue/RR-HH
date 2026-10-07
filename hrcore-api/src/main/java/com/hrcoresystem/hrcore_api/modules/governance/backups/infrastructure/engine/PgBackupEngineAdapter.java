package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.engine;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.config.BackupProperties;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupExecutionException;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupArtifact;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.ChecksumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PgBackupEngineAdapter implements BackupEngine {

    private static final Logger log = LoggerFactory.getLogger(PgBackupEngineAdapter.class);

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
    public String restoreSchema(String schemaName, Path source) {
        validateSqlIdentifier(schemaName, "schemaName");

        if (!Files.exists(source)) {
            throw new BackupExecutionException("Backup artifact does not exist: " + source);
        }

        executePsql("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE;");

        return runRestoreTolerant(
                List.of(
                        properties.getPgRestorePath(),
                        "-h", properties.getHost(),
                        "-p", properties.getPort(),
                        "-U", properties.getUsername(),
                        "-d", properties.getDatabase(),
                        "--no-owner",
                        "--no-privileges",
                        source.toString()
                ),
                "pg_restore schema " + schemaName
        );
    }

    @Override
    public String restoreFullDatabase(Path source) {
        if (!Files.exists(source)) {
            throw new BackupExecutionException("Backup artifact does not exist: " + source);
        }

        return runRestoreTolerant(buildRestoreFullDatabaseCommand(source), "pg_restore full database");
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

    private void runStrict(List<String> command, String description) {
        CommandExecution execution = runCommand(command, description);
        if (execution.exitCode() != 0) {
            throw new BackupExecutionException(
                    "Command failed: " + description +
                            ". Exit code: " + execution.exitCode() +
                            ". Output: " + truncate(execution.output())
            );
        }
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
        try {
            log.info("Executing backup command: {}", description);

            ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(command));
            processBuilder.environment().put("PGPASSWORD", properties.getPassword());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            return new CommandExecution(exitCode, output.toString());
        } catch (BackupExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new BackupExecutionException("Unable to execute command: " + description, e);
        }
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

    private record CommandExecution(int exitCode, String output) {
    }
}
