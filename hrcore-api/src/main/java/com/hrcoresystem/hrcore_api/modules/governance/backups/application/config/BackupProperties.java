package com.hrcoresystem.hrcore_api.modules.governance.backups.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.backups")
public class BackupProperties {

    private String localPath = "/storage/backups";
    private String pgDumpPath = "pg_dump";
    private String pgRestorePath = "pg_restore";
    private String psqlPath = "psql";
    private String host = "db";
    private String port = "5432";
    private String database = "hrcore_db";
    private String username = "hrcore_user";
    private String password = "hrcore_password";

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getPgDumpPath() {
        return pgDumpPath;
    }

    public void setPgDumpPath(String pgDumpPath) {
        this.pgDumpPath = pgDumpPath;
    }

    public String getPgRestorePath() {
        return pgRestorePath;
    }

    public void setPgRestorePath(String pgRestorePath) {
        this.pgRestorePath = pgRestorePath;
    }

    public String getPsqlPath() {
        return psqlPath;
    }

    public void setPsqlPath(String psqlPath) {
        this.psqlPath = psqlPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
