package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.bootstrap.platform.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class LeavePermissionSeedsTest {
    @Test void bootstrapSeedsExactlyTwentyTwoLeavePermissionsWithoutOverwritingExistingRows() throws Exception {
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
        Set<String> leaves = new HashSet<>();
        for (String code : codes.getAllValues()) if (code.startsWith("leaves.")) leaves.add(code);
        assertEquals(22, leaves.size());
        assertTrue(leaves.contains("leaves.balances.adjust"));
        assertTrue(leaves.contains("leaves.attachments.download"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection, atLeastOnce()).prepareStatement(sql.capture());
        assertTrue(sql.getAllValues().stream().anyMatch(query -> query.contains("ON CONFLICT (code) DO NOTHING")));
    }
}
