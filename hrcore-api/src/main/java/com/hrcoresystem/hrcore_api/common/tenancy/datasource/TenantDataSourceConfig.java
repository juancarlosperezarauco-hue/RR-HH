package com.hrcoresystem.hrcore_api.common.tenancy.datasource;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class TenantDataSourceConfig {

    @Bean(name = "targetDataSource")
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource targetDataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource tenantAwareDataSource(
            @Qualifier("targetDataSource") DataSource targetDataSource,
            TenancyProperties tenancyProperties
    ) {
        return new TenantAwareDataSource(targetDataSource, tenancyProperties);
    }
}