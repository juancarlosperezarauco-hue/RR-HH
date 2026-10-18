package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.context.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component public class ContractExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ContractExpirationScheduler.class);
    private final PlatformTenantRepository tenants; private final ContractExpirationTenantExecutor executor;
    public ContractExpirationScheduler(PlatformTenantRepository tenants,ContractExpirationTenantExecutor executor){this.tenants=tenants;this.executor=executor;}
    @Scheduled(fixedDelayString="${hrcore.contracts.expiration-ms:3600000}") public void expireTenants(){for(PlatformTenant tenant:tenants.findAll()){if(!tenant.active()||tenant.status()!=PlatformTenantStatus.ACTIVE||tenant.schemaName()==null||tenant.schemaName().isBlank())continue;try{TenantContextHolder.set(new TenantContext(tenant.slug(),tenant.schemaName(),false));executor.execute();}catch(Exception ex){log.error("Error procesando expiración contractual del tenant {}",tenant.slug(),ex);}finally{TenantContextHolder.clear();}}}
}
