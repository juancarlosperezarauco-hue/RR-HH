package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service;
import com.hrcoresystem.hrcore_api.common.exception.InvalidBusinessStateException; import org.springframework.stereotype.Service;
@Service public class TenantHrOperationalGuard { private final TenantSetupStatusService status; public TenantHrOperationalGuard(TenantSetupStatusService s){status=s;} public void assertConfigured(){if(!status.execute().configured()) throw new InvalidBusinessStateException("Debe completar la configuración de la empresa antes de realizar esta operación");} }
