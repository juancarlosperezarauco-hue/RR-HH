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
import java.lang.reflect.Proxy;

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
    try {
      applySchema(connection, schemaName);
      return resetOnClose(connection);
    } catch (SQLException exception) {
      try { connection.close(); } catch (SQLException closeException) { exception.addSuppressed(closeException); }
      throw exception;
    }
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    String schemaName = resolveSchemaName();
    Connection connection = delegate.getConnection(username, password);
    try {
      applySchema(connection, schemaName);
      return resetOnClose(connection);
    } catch (SQLException exception) {
      try { connection.close(); } catch (SQLException closeException) { exception.addSuppressed(closeException); }
      throw exception;
    }
  }

  private void applySchema(Connection connection, String schemaName) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + schemaName + ", " + tenancyProperties.getPublicSchema());
    }
  }

  private Connection resetOnClose(Connection connection) {
    return (Connection) Proxy.newProxyInstance(
        Connection.class.getClassLoader(),
        new Class<?>[]{Connection.class},
        (proxy, method, args) -> {
          if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
            try (Statement statement = connection.createStatement()) {
              statement.execute("SET search_path TO " + tenancyProperties.getPublicSchema());
            } finally {
              connection.close();
            }
            return null;
          }
          return method.invoke(connection, args);
        });
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
              requestPath + "'. Verify the tenant slug matches an existing tenant before retrying.");
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
