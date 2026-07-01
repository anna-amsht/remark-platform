package com.remark.platform.service;

import com.remark.platform.client.UserProfileResponse;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.AssignMasterRequest;
import com.remark.platform.dto.CreateRemarkRequest;
import com.remark.platform.dto.ResolveRemarkRequest;
import com.remark.platform.dto.SetRemarkCalendarEventRequest;
import com.remark.platform.dto.SetEstimatedResolutionRequest;
import com.remark.platform.entity.EstimatedResolutionUnit;
import com.remark.platform.entity.Remark;
import com.remark.platform.entity.RemarkCalendarEntry;
import com.remark.platform.entity.RemarkStatus;
import com.remark.platform.entity.RemarkStatusHistory;
import com.remark.platform.exceptions.InvalidRemarkTransitionException;
import com.remark.platform.exceptions.RemarkAccessDeniedException;
import com.remark.platform.repository.RemarkCalendarEntryRepository;
import com.remark.platform.repository.RemarkRepository;
import com.remark.platform.repository.RemarkStatusHistoryRepository;
import com.remark.platform.security.RoleCode;
import com.remark.platform.security.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemarkServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Minsk");

    @Mock
    private RemarkRepository remarkRepository;

    @Mock
    private RemarkCalendarEntryRepository remarkCalendarEntryRepository;

    @Mock
    private RemarkStatusHistoryRepository remarkStatusHistoryRepository;

    @Mock
    private RemarkNumberGenerator remarkNumberGenerator;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RemarkService remarkService;

    private final UUID branchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID applicantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID masterId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID remarkId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void createStoresNewRemarkAndHistory() {
        when(remarkNumberGenerator.nextRemarkNumber()).thenReturn("REM-2026-00001");
        when(remarkRepository.save(any(Remark.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = remarkService.create(
                new CreateRemarkRequest(" Building A ", " Broken window ", LocalDate.now()),
                applicant()
        );

        assertThat(response.remarkNumber()).isEqualTo("REM-2026-00001");
        assertThat(response.objectBuilding()).isEqualTo("Building A");
        assertThat(response.description()).isEqualTo("Broken window");
        assertThat(response.status()).isEqualTo(RemarkStatus.NEW);
        verify(remarkStatusHistoryRepository).save(any());
    }

    @Test
    void createRejectsMaster() {
        assertThatThrownBy(() -> remarkService.create(
                new CreateRemarkRequest("Building", "Description", LocalDate.now()),
                master()
        )).isInstanceOf(RemarkAccessDeniedException.class);
    }

    @Test
    void assignMasterMovesNewRemarkToAssigned() {
        Remark remark = remark(RemarkStatus.NEW);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkRepository.save(remark)).thenReturn(remark);

        var response = remarkService.assignMaster(remarkId, new AssignMasterRequest(masterId), branchHead());

        assertThat(response.status()).isEqualTo(RemarkStatus.ASSIGNED);
        assertThat(response.assignedMasterId()).isEqualTo(masterId);
        verify(userServiceClient).ensureActiveMasterInBranch(masterId, branchId);
        verify(remarkStatusHistoryRepository).save(any());
    }

    @Test
    void assignMasterRejectsApplicant() {
        assertThatThrownBy(() -> remarkService.assignMaster(remarkId, new AssignMasterRequest(masterId), applicant()))
                .isInstanceOf(RemarkAccessDeniedException.class);
    }

    @Test
    void startWorkRequiresEstimatedResolution() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        remark.setAssignedMasterId(masterId);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));

        assertThatThrownBy(() -> remarkService.startWork(remarkId, master()))
                .isInstanceOf(InvalidRemarkTransitionException.class)
                .hasMessageContaining("Estimated resolution");
    }

    @Test
    void setEstimatedResolutionStoresDurationInDays() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        remark.setAssignedMasterId(masterId);
        remark.setCreatedAt(LocalDateTime.now(BUSINESS_ZONE).minusDays(1).atZone(BUSINESS_ZONE).toInstant());
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkRepository.save(remark)).thenReturn(remark);

        var response = remarkService.setEstimatedResolution(
                remarkId,
                new SetEstimatedResolutionRequest(LocalDateTime.now(BUSINESS_ZONE).plusDays(1)),
                master()
        );

        assertThat(response.estimatedResolutionDuration()).isEqualTo(2);
        assertThat(response.estimatedResolutionUnit()).isEqualTo(EstimatedResolutionUnit.DAYS);
        verify(remarkStatusHistoryRepository).save(any());
    }

    @Test
    void startWorkMovesAssignedRemarkToInProgress() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        remark.setAssignedMasterId(masterId);
        remark.setEstimatedResolutionAt(LocalDateTime.now(BUSINESS_ZONE).plusDays(1));
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkRepository.save(remark)).thenReturn(remark);

        var response = remarkService.startWork(remarkId, master());

        assertThat(response.status()).isEqualTo(RemarkStatus.IN_PROGRESS);
    }

    @Test
    void resolvePublishesResolvedEvent() {
        Remark remark = remark(RemarkStatus.IN_PROGRESS);
        remark.setAssignedMasterId(masterId);
        remark.setEstimatedResolutionAt(LocalDateTime.now(BUSINESS_ZONE).plusDays(1));
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkRepository.save(remark)).thenReturn(remark);
        when(userServiceClient.getActiveUser(applicantId)).thenReturn(new UserProfileResponse(
                applicantId,
                "creator@example.com",
                "Creator",
                "Engineer",
                branchId,
                "ACTIVE",
                Set.of(RoleCode.APPLICANT)
        ));

        var response = remarkService.resolve(remarkId, new ResolveRemarkRequest(" Fixed "), master());

        assertThat(response.status()).isEqualTo(RemarkStatus.RESOLVED);
        assertThat(response.resolutionMessage()).isEqualTo("Fixed");
        verify(eventPublisher).publishEvent(any(RemarkResolvedApplicationEvent.class));
    }

    @Test
    void findByIdAllowsApplicantFromSameBranch() {
        Remark remark = remark(RemarkStatus.NEW);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));

        var response = remarkService.findById(remarkId, applicant());

        assertThat(response.id()).isEqualTo(remarkId);
    }

    @Test
    void findByIdRejectsMasterWhenRemarkIsNotAssignedToHim() {
        Remark remark = remark(RemarkStatus.NEW);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));

        assertThatThrownBy(() -> remarkService.findById(remarkId, master()))
                .isInstanceOf(RemarkAccessDeniedException.class)
                .hasMessageContaining("cannot view");
    }

    @Test
    void setCalendarEventCreatesCustomEntry() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        remark.setAssignedMasterId(masterId);
        remark.setEstimatedResolutionAt(LocalDateTime.now(BUSINESS_ZONE).plusDays(2));
        LocalDate workDate = LocalDate.now(BUSINESS_ZONE).plusDays(1);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkCalendarEntryRepository.findByRemarkIdAndMasterIdAndWorkDate(remarkId, masterId, workDate))
                .thenReturn(Optional.empty());
        when(remarkCalendarEntryRepository.save(any(RemarkCalendarEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = remarkService.setCalendarEvent(
                remarkId,
                new SetRemarkCalendarEventRequest(workDate, LocalTime.of(11, 0), LocalTime.of(13, 0)),
                master()
        );

        assertThat(response.customTime()).isTrue();
        assertThat(response.workStartTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.workEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void setCalendarEventRejectsInvalidTimeRange() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        remark.setAssignedMasterId(masterId);
        remark.setEstimatedResolutionAt(LocalDateTime.now(BUSINESS_ZONE).plusDays(2));
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));

        assertThatThrownBy(() -> remarkService.setCalendarEvent(
                remarkId,
                new SetRemarkCalendarEventRequest(
                        LocalDate.now(BUSINESS_ZONE).plusDays(1),
                        LocalTime.of(13, 0),
                        LocalTime.of(13, 0)
                ),
                master()
        )).isInstanceOf(InvalidRemarkTransitionException.class)
                .hasMessageContaining("end time");
    }

    @Test
    void removeCalendarEventHidesExistingEntry() {
        Remark remark = remark(RemarkStatus.IN_PROGRESS);
        remark.setAssignedMasterId(masterId);
        remark.setEstimatedResolutionAt(LocalDateTime.now(BUSINESS_ZONE).plusDays(2));
        LocalDate workDate = LocalDate.now(BUSINESS_ZONE).plusDays(1);
        RemarkCalendarEntry entry = calendarEntry(workDate);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkCalendarEntryRepository.findByRemarkIdAndMasterIdAndWorkDate(remarkId, masterId, workDate))
                .thenReturn(Optional.of(entry));

        remarkService.removeCalendarEvent(remarkId, workDate, master());

        assertThat(entry.isHidden()).isTrue();
        verify(remarkCalendarEntryRepository).save(entry);
    }

    @Test
    void findCalendarEventsBuildsDefaultAndCustomEvents() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        remark.setAssignedMasterId(masterId);
        remark.setEstimatedResolutionAt(LocalDateTime.now(BUSINESS_ZONE).plusDays(1));
        RemarkCalendarEntry entry = calendarEntry(LocalDate.now(BUSINESS_ZONE));
        when(remarkCalendarEntryRepository.findByMasterIdAndWorkDateGreaterThanEqualAndWorkDateLessThan(
                masterId,
                LocalDate.now(BUSINESS_ZONE),
                LocalDate.now(BUSINESS_ZONE).plusDays(3)
        )).thenReturn(List.of(entry));
        when(remarkRepository.findByAssignedMasterIdOrderByCreatedAtDesc(masterId)).thenReturn(List.of(remark));

        var events = remarkService.findCalendarEvents(
                master(),
                LocalDateTime.now(BUSINESS_ZONE).with(LocalTime.MIN),
                LocalDateTime.now(BUSINESS_ZONE).plusDays(2).with(LocalTime.MIN)
        );

        assertThat(events).isNotEmpty();
        assertThat(events).anyMatch(event -> event.customTime());
    }

    @Test
    void findCalendarEventsRejectsNonMaster() {
        assertThatThrownBy(() -> remarkService.findCalendarEvents(applicant(), null, null))
                .isInstanceOf(RemarkAccessDeniedException.class)
                .hasMessageContaining("Only master");
    }

    @Test
    void findByIdInternalIncludesAssignerFromHistory() {
        Remark remark = remark(RemarkStatus.ASSIGNED);
        RemarkStatusHistory history = new RemarkStatusHistory();
        UUID assignerId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        history.setToStatus(RemarkStatus.ASSIGNED);
        history.setChangedByUserId(assignerId);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkStatusHistoryRepository.findByRemarkIdOrderByChangedAtAsc(remarkId)).thenReturn(List.of(history));

        var response = remarkService.findByIdInternal(remarkId);

        assertThat(response.assignedByUserId()).isEqualTo(assignerId);
    }

    @Test
    void markReportSentStoresTimestamp() {
        Remark remark = remark(RemarkStatus.RESOLVED);
        when(remarkRepository.findById(remarkId)).thenReturn(Optional.of(remark));
        when(remarkRepository.save(remark)).thenReturn(remark);
        when(remarkStatusHistoryRepository.findByRemarkIdOrderByChangedAtAsc(remarkId)).thenReturn(List.of());

        var response = remarkService.markReportSent(remarkId);

        assertThat(response.reportSentAt()).isNotNull();
    }

    private Remark persisted(Remark remark) {
        remark.setId(remarkId);
        if (remark.getCreatedAt() == null) {
            remark.setCreatedAt(Instant.now());
        }
        if (remark.getUpdatedAt() == null) {
            remark.setUpdatedAt(Instant.now());
        }
        return remark;
    }

    private Remark remark(RemarkStatus status) {
        Remark remark = new Remark();
        remark.setId(remarkId);
        remark.setRemarkNumber("REM-2026-00001");
        remark.setBranchId(branchId);
        remark.setObjectBuilding("Building A");
        remark.setDescription("Broken window");
        remark.setDiscoveredAt(LocalDate.now());
        remark.setCreatedByUserId(applicantId);
        remark.setStatus(status);
        remark.setCreatedAt(LocalDateTime.now(BUSINESS_ZONE).minusHours(2).atZone(BUSINESS_ZONE).toInstant());
        remark.setUpdatedAt(Instant.now());
        return remark;
    }

    private RemarkCalendarEntry calendarEntry(LocalDate workDate) {
        RemarkCalendarEntry entry = new RemarkCalendarEntry();
        entry.setRemarkId(remarkId);
        entry.setMasterId(masterId);
        entry.setWorkDate(workDate);
        entry.setWorkStartTime(LocalTime.of(10, 0));
        entry.setWorkEndTime(LocalTime.of(12, 0));
        return entry;
    }

    private UserContext applicant() {
        return new UserContext(applicantId, branchId, Set.of(RoleCode.APPLICANT));
    }

    private UserContext branchHead() {
        return new UserContext(UUID.fromString("55555555-5555-5555-5555-555555555555"), branchId, Set.of(RoleCode.BRANCH_HEAD));
    }

    private UserContext master() {
        return new UserContext(masterId, branchId, Set.of(RoleCode.MASTER));
    }
}
