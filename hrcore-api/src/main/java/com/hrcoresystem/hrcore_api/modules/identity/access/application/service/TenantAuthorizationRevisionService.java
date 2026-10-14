package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import org.springframework.stereotype.Service;
@Service public class TenantAuthorizationRevisionService { private final TenantAuthorizationStateRepository repository; public TenantAuthorizationRevisionService(TenantAuthorizationStateRepository repository){this.repository=repository;} public void incrementAuthorizationVersion(){if(repository.incrementVersion()!=1)throw new IllegalStateException("Tenant authorization state is missing");} }
