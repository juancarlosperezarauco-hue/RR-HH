package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrcoresystem.hrcore_api.bootstrap.platform.PlatformBootstrapService;
import com.hrcoresystem.hrcore_api.bootstrap.platform.SuperadminBootstrapProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class ShiftPermissionSeedsTest {
    @Test
    void bootstrapAcceptsExistingUnderscoreCodesAndRegistersAllShiftAuthoritiesIdempotently() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        PlatformBootstrapService bootstrap = new PlatformBootstrapService(
                dataSource, mock(PasswordEncoder.class), new SuperadminBootstrapProperties());
        bootstrap.seedBaseSystemPermissions();

        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeast(23)).setString(eq(1), codes.capture());
        List<String> registered = codes.getAllValues();
        Set<String> shiftCodes = new HashSet<>();
        for (String code : registered) {
            if (code.startsWith("shifts.")) shiftCodes.add(code);
        }
        assertEquals(23, shiftCodes.size());
        assertTrue(registered.contains("employee_files.documents.confidential.read"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection, atLeastOnce()).prepareStatement(sql.capture());
        assertTrue(sql.getAllValues().stream().allMatch(query -> query.contains("ON CONFLICT (code)")));
    }
}
