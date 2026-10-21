package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiPlannerClient;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConversationTurnPersistenceTest {
    @Mock private AiConversationJpaRepository conversations;
    @Mock private AiMessageJpaRepository messages;
    @Mock private AiReportRunJpaRepository reportRuns;

    @Test
    void reservationCapturesOnlyBoundedMessageMetadataAndLatestSuccessfulPlan() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        AiConversationEntity conversation = new AiConversationEntity();
        conversation.setId(conversationId);
        conversation.setOwnerUserId(ownerId);
        conversation.setVersion(7);
        when(conversations.findByIdAndOwnerUserId(conversationId, ownerId))
                .thenReturn(Optional.of(conversation));
        when(conversations.saveAndFlush(any(AiConversationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.findByConversationIdAndClientMessageId(conversationId, clientMessageId))
                .thenReturn(Optional.empty());
        when(messages.existsByConversationIdAndRoleAndState(conversationId, "USER", "PENDING"))
                .thenReturn(false);

        AiReportRunEntity latestRun = new AiReportRunEntity();
        latestRun.setId(UUID.randomUUID());
        latestRun.setQueryPlan(mapper.readTree("""
                {"version":"1","catalogVersion":"1","dataset":"employees.roster",
                 "mode":"detail","asOfDate":"2026-10-01","select":["employee.full_name"],
                 "metrics":[],"filters":[],"groupBy":[],"having":[],"orderBy":[],
                 "page":{"number":0,"size":20},"visualization":{"type":"table"}}
                """));
        latestRun.setPlanVersion("1");
        latestRun.setCatalogVersion("1");
        when(reportRuns.findFirstByConversationIdAndStateOrderByFinishedAtDescIdDesc(
                conversationId, "SUCCEEDED")).thenReturn(Optional.of(latestRun));

        AiMessageEntity assistant = message(conversationId, "ASSISTANT", "private answer text",
                "REPORT", "COMPLETED");
        AiMessageEntity user = message(conversationId, "USER", "private question text", "TEXT", "COMPLETED");
        when(messages.findAllByConversationIdOrderByCreatedAtDescIdDesc(
                eq(conversationId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assistant, user), PageRequest.of(0, 4), 2));
        when(messages.saveAndFlush(any(AiMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiConversationTurnPersistence persistence = new AiConversationTurnPersistence(
                conversations, messages, reportRuns, mapper, 4);
        AiConversationTurnPersistence.Reservation reservation = persistence.reserve(conversationId, ownerId,
                clientMessageId, "nueva pregunta", Duration.ofMinutes(2));

        assertFalse(reservation.replay());
        assertEquals(7, reservation.conversationVersion());
        AiPlannerClient.ConversationContext context = reservation.conversationContext();
        assertTrue(context.hasUsablePlan());
        assertEquals(latestRun.getQueryPlan(), context.lastSuccessfulPlan());
        assertEquals("1", context.catalogVersion());
        assertEquals(List.of(
                new AiPlannerClient.MessageMetadata("USER", "TEXT", "COMPLETED"),
                new AiPlannerClient.MessageMetadata("ASSISTANT", "REPORT", "COMPLETED")),
                context.recentMessages());
        assertFalse(context.toString().contains("private question text"));
        assertFalse(context.toString().contains("private answer text"));

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(messages).findAllByConversationIdOrderByCreatedAtDescIdDesc(eq(conversationId), page.capture());
        assertEquals(4, page.getValue().getPageSize());
    }

    @Test
    void completionRejectsAReservationWhoseConversationVersionIsStale() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        AiConversationEntity conversation = new AiConversationEntity();
        conversation.setId(conversationId);
        conversation.setOwnerUserId(ownerId);
        conversation.setVersion(9);
        when(conversations.findByIdAndOwnerUserId(conversationId, ownerId))
                .thenReturn(Optional.of(conversation));

        AiConversationTurnPersistence persistence = new AiConversationTurnPersistence(
                conversations, messages, reportRuns, new ObjectMapper(), 4);
        assertThrows(BusinessConflictException.class, () -> persistence.complete(conversationId, ownerId,
                UUID.randomUUID(), 8, "COMPLETED", "REPORT", "Reporte", null, null, 1));
        verifyNoInteractions(messages, reportRuns);
    }

    private static AiMessageEntity message(UUID conversationId, String role, String content,
                                           String type, String state) {
        AiMessageEntity message = new AiMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageType(type);
        message.setState(state);
        message.setCreatedAt(Instant.now());
        return message;
    }
}
