package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListTenantUsersUseCaseTest {

    private TenantUserRepository tenantUserRepository;
    private TenantUserMapper tenantUserMapper;
    private ListTenantUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        tenantUserRepository = mock(TenantUserRepository.class);
        tenantUserMapper = new TenantUserMapper(null);
        useCase = new ListTenantUsersUseCase(tenantUserRepository, tenantUserMapper);
    }

    @Test
    @DisplayName("Criterio 3: Consulta y filtra usuarios por nombre, correo y estado")
    void shouldFilterUsersByNameEmailAndStatus() {
        TenantUser user1 = new TenantUser(
                UUID.randomUUID(),
                "carlos.rojas@empresa.com",
                "pwd",
                "Carlos",
                "Rojas",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        TenantUser user2 = new TenantUser(
                UUID.randomUUID(),
                "maria.lopez@empresa.com",
                "pwd",
                "María",
                "López",
                false,
                TenantUserStatus.INACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        TenantUser user3 = new TenantUser(
                UUID.randomUUID(),
                "carlos.mendoza@empresa.com",
                "pwd",
                "Carlos",
                "Mendoza",
                false,
                TenantUserStatus.PENDING,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findAll()).thenReturn(List.of(user1, user2, user3));

        // 1. Filtrar por texto "carlos"
        List<TenantUserResponse> filteredByText = useCase.execute("carlos", null, null);
        assertEquals(2, filteredByText.size());
        assertTrue(filteredByText.stream().allMatch(u -> u.email().contains("carlos")));

        // 2. Filtrar por estado ACTIVE
        List<TenantUserResponse> filteredByStatus = useCase.execute(null, TenantUserStatus.ACTIVE, null);
        assertEquals(1, filteredByStatus.size());
        assertEquals("carlos.rojas@empresa.com", filteredByStatus.getFirst().email());

        // 3. Filtrar por texto "carlos" y estado PENDING
        List<TenantUserResponse> filteredBoth = useCase.execute("carlos", TenantUserStatus.PENDING, false);
        assertEquals(1, filteredBoth.size());
        assertEquals("carlos.mendoza@empresa.com", filteredBoth.getFirst().email());
    }
}
