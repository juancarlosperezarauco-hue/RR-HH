package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
@Service public class TenantOwnershipTransferService { public void transferOwnership(UUID currentOwnerId,UUID newOwnerId){throw new AccessDeniedException("Ownership transfer is not enabled");} }
