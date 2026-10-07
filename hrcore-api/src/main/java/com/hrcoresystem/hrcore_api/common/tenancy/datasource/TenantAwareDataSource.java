package com.hrcoresystem.hrcore_api.common.tenancy.datasource;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TenantAwareDataSource extends AbstractDataSource {

    private final DataSource delegate;
    private final TenancyProperties tenancyProperties;

    public TenantAwareDataSource(DataSource delegate, TenancyProperties tenancyProperties) {
        this.delegate = delegate;
        this.tenancyProperties = tenancyProperties;
    }

    @Override
    public Connection getConnection() throws SQLException {
        String schemaName = resolveSchemaName();
        Connection connection = delegate.getConnection();
        applySchema(connection, schemaName);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        String schemaName = resolveSchemaName();
        Connection connection = delegate.getConnection(username, password);
        applySchema(connection, schemaName);
        return connection;
    }

    private void applySchema(Connection connection, String schemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schemaName + ", " + tenancyProperties.getPublicSchema());
        }
    }

    private String resolveSchemaName() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return TenantContextHolder.getCurrentSchemaOrDefault(tenancyProperties.getPublicSchema());
        }

        String requestPath = request.getRequestURI();
        if (tenancyProperties.usesPublicSchema(requestPath)) {
            return tenancyProperties.getPublicSchema();
        }

        TenantContext tenantContext = TenantContextHolder.get();
        if (tenantContext == null
                || tenantContext.publicRequest()
                || tenantContext.schemaName() == null
                || tenantContext.schemaName().isBlank()
                || tenancyProperties.getPublicSchema().equalsIgnoreCase(tenantContext.schemaName())) {
            throw new TenantResolutionException(
                    "Tenant header '" + tenancyProperties.getHeaderName() + "' is missing or invalid for request path '" +
                            requestPath + "'. Verify the tenant slug matches an existing tenant before retrying."
            );
        }

        return tenantContext.schemaName();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        return servletRequestAttributes.getRequest();
    }
}
