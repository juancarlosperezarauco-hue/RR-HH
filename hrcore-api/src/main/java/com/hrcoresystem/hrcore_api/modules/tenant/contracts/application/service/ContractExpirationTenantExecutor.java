package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ContractExpirationTenantExecutor { private final ContractExpirationService expiration; private final TenantTimezoneService timezone; public ContractExpirationTenantExecutor(ContractExpirationService expiration,TenantTimezoneService timezone){this.expiration=expiration;this.timezone=timezone;} @Transactional public int execute(){return expiration.expireDue(timezone.today());} }
