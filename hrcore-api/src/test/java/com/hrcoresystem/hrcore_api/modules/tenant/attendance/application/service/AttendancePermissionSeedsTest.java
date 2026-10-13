package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.bootstrap.platform.PlatformBootstrapService;
import com.hrcoresystem.hrcore_api.bootstrap.platform.SuperadminBootstrapProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AttendancePermissionSeedsTest {
    @Test
    void bootstrapRegistersExactlyTheAttendanceAuthoritiesIdempotently() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        new PlatformBootstrapService(dataSource, mock(PasswordEncoder.class),
                new SuperadminBootstrapProperties()).seedBaseSystemPermissions();

        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).setString(eq(1), codes.capture());
        Set<String> attendance = new HashSet<>();
        for (String code : codes.getAllValues()) if (code.startsWith("attendance.")) attendance.add(code);
        assertEquals(19, attendance.size());
        assertTrue(attendance.contains("attendance.punches.manage"));
        assertTrue(attendance.contains("attendance.overtime.approve"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection, atLeastOnce()).prepareStatement(sql.capture());
        assertTrue(sql.getAllValues().stream().allMatch(query -> query.contains("ON CONFLICT (code)")));
    }
}
