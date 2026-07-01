package com.remark.platform.service;

import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.dto.AssignMasterRequest;
import com.remark.platform.dto.CreateRemarkRequest;
import com.remark.platform.dto.PagedRemarkResponse;
import com.remark.platform.dto.RemarkCalendarEventResponse;
import com.remark.platform.dto.RemarkResponse;
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
import com.remark.platform.exceptions.RemarkNotFoundException;
import com.remark.platform.repository.RemarkCalendarEntryRepository;
import com.remark.platform.repository.RemarkRepository;
import com.remark.platform.repository.RemarkStatusHistoryRepository;
import com.remark.platform.security.RoleCode;
import com.remark.platform.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RemarkService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Minsk");
    private static final LocalTime DEFAULT_WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORK_END_TIME = LocalTime.of(10, 0);
    private static final int MAX_PAGE_SIZE = 100;

    private final RemarkRepository remarkRepository;
    private final RemarkCalendarEntryRepository remarkCalendarEntryRepository;
    private final RemarkStatusHistoryRepository remarkStatusHistoryRepository;
    private final RemarkNumberGenerator remarkNumberGenerator;
    private final UserServiceClient userServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    public RemarkService(
            RemarkRepository remarkRepository,
            RemarkCalendarEntryRepository remarkCalendarEntryRepository,
            RemarkStatusHistoryRepository remarkStatusHistoryRepository,
            RemarkNumberGenerator remarkNumberGenerator,
            UserServiceClient userServiceClient,
            ApplicationEventPublisher eventPublisher
    ) {
        this.remarkRepository = remarkRepository;
        this.remarkCalendarEntryRepository = remarkCalendarEntryRepository;
        this.remarkStatusHistoryRepository = remarkStatusHistoryRepository;
        this.remarkNumberGenerator = remarkNumberGenerator;
        this.userServiceClient = userServiceClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RemarkResponse create(CreateRemarkRequest request, UserContext actor) {
        assertCanCreate(actor);

        UUID branchId = actor.requireBranchId();

        Remark remark = new Remark();
        remark.setRemarkNumber(remarkNumberGenerator.nextRemarkNumber());
        remark.setBranchId(branchId);
        remark.setObjectBuilding(request.objectBuilding().trim());
        remark.setDescription(request.description().trim());
        remark.setDiscoveredAt(request.discoveredAt());
        remark.setCreatedByUserId(actor.userId());
        remark.setStatus(RemarkStatus.NEW);

        Remark saved = remarkRepository.save(remark);
        saveHistory(saved.getId(), null, RemarkStatus.NEW, actor.userId(), "Remark created");
        return RemarkMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RemarkResponse findById(UUID id, UserContext actor) {
        Remark remark = getRemark(id);
        assertCanView(remark, actor);
        return RemarkMapper.toResponse(remark);
    }

    @Transactional(readOnly = true)
    public List<RemarkCalendarEventResponse> findCalendarEvents(
            UserContext actor,
            LocalDateTime from,
            LocalDateTime to
    ) {
        assertCanViewCalendar(actor);
        assertValidCalendarRange(from, to);
        LocalDateTime rangeFrom = from != null ? from : LocalDateTime.now(BUSINESS_ZONE).minusMonths(1);
        LocalDateTime rangeTo = to != null ? to : rangeFrom.plusMonths(3);
        LocalDate fromDate = rangeFrom.toLocalDate();
        LocalDate toDate = rangeTo.toLocalDate().plusDays(1);
        Map<CalendarEntryKey, RemarkCalendarEntry> entries = remarkCalendarEntryRepository
                .findByMasterIdAndWorkDateGreaterThanEqualAndWorkDateLessThan(actor.userId(), fromDate, toDate)
                .stream()
                .collect(Collectors.toMap(
                        entry -> new CalendarEntryKey(entry.getRemarkId(), entry.getWorkDate()),
                        Function.identity(),
                        (existing, duplicate) -> existing
                ));

        return remarkRepository.findByAssignedMasterIdOrderByCreatedAtDesc(actor.userId())
                .stream()
                .filter(remark -> remark.getEstimatedResolutionAt() != null)
                .filter(remark -> remark.getStatus() == RemarkStatus.ASSIGNED
                        || remark.getStatus() == RemarkStatus.IN_PROGRESS)
                .filter(remark -> overlapsCalendarRange(remark, rangeFrom, rangeTo))
                .flatMap(remark -> buildCalendarEvents(remark, fromDate, toDate, entries).stream())
                .sorted(Comparator.comparing(RemarkCalendarEventResponse::start)
                        .thenComparing(RemarkCalendarEventResponse::remarkNumber))
                .toList();
    }

    @Transactional
    public RemarkCalendarEventResponse setCalendarEvent(
            UUID id,
            SetRemarkCalendarEventRequest request,
            UserContext actor
    ) {
        Remark remark = getRemark(id);
        assertAssignedMaster(remark, actor);
        assertCanEstimateResolution(remark);
        assertEstimatedResolutionSet(remark);
        assertCalendarDateWithinResolutionPeriod(remark, request.workDate());
        if (!request.workEndTime().isAfter(request.workStartTime())) {
            throw new InvalidRemarkTransitionException("Calendar event end time must be after start time");
        }

        RemarkCalendarEntry entry = remarkCalendarEntryRepository
                .findByRemarkIdAndMasterIdAndWorkDate(remark.getId(), actor.userId(), request.workDate())
                .orElseGet(() -> {
                    RemarkCalendarEntry created = new RemarkCalendarEntry();
                    created.setRemarkId(remark.getId());
                    created.setMasterId(actor.userId());
                    created.setWorkDate(request.workDate());
                    return created;
                });
        entry.setWorkStartTime(request.workStartTime());
        entry.setWorkEndTime(request.workEndTime());
        entry.setHidden(false);
        return toCalendarEventResponse(remark, remarkCalendarEntryRepository.save(entry));
    }

    @Transactional
    public void removeCalendarEvent(UUID id, LocalDate workDate, UserContext actor) {
        Remark remark = getRemark(id);
        assertAssignedMaster(remark, actor);
        assertCanEstimateResolution(remark);
        assertEstimatedResolutionSet(remark);
        assertCalendarDateWithinResolutionPeriod(remark, workDate);

        RemarkCalendarEntry entry = remarkCalendarEntryRepository
                .findByRemarkIdAndMasterIdAndWorkDate(remark.getId(), actor.userId(), workDate)
                .orElseGet(() -> {
                    RemarkCalendarEntry created = new RemarkCalendarEntry();
                    created.setRemarkId(remark.getId());
                    created.setMasterId(actor.userId());
                    created.setWorkDate(workDate);
                    created.setWorkStartTime(DEFAULT_WORK_START_TIME);
                    created.setWorkEndTime(DEFAULT_WORK_END_TIME);
                    return created;
                });
        entry.setHidden(true);
        remarkCalendarEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PagedRemarkResponse findAll(
            UserContext actor,
            RemarkStatus status,
            UUID branchIdFilter,
            int page,
            int size,
            String search
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String normalizedSearch = normalizeSearch(search);
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<Remark> remarks;
        if (actor.hasRole(RoleCode.MASTER) && !actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD, RoleCode.APPLICANT)) {
            remarks = remarkRepository.searchRemarksByMaster(actor.userId(), status, normalizedSearch, pageable);
        } else if (actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD)) {
            remarks = remarkRepository.searchRemarks(branchIdFilter, status, normalizedSearch, pageable);
        } else {
            UUID branchId = actor.requireBranchId();
            remarks = remarkRepository.searchRemarks(branchId, status, normalizedSearch, pageable);
        }

        return toPagedResponse(remarks);
    }

    @Transactional
    public RemarkResponse assignMaster(UUID id, AssignMasterRequest request, UserContext actor) {
        assertCanAssign(actor);
        Remark remark = getRemark(id);
        assertSameBranch(remark, actor);
        assertStatus(remark, RemarkStatus.NEW);

        userServiceClient.ensureActiveMasterInBranch(request.masterId(), remark.getBranchId());

        remark.setAssignedMasterId(request.masterId());
        transition(remark, RemarkStatus.ASSIGNED, actor.userId(), "Master assigned");
        return RemarkMapper.toResponse(remarkRepository.save(remark));
    }

    @Transactional
    public RemarkResponse startWork(UUID id, UserContext actor) {
        Remark remark = getRemark(id);
        assertAssignedMaster(remark, actor);
        assertStatus(remark, RemarkStatus.ASSIGNED);
        assertEstimatedResolutionSet(remark);

        transition(remark, RemarkStatus.IN_PROGRESS, actor.userId(), "Work started");
        return RemarkMapper.toResponse(remarkRepository.save(remark));
    }

    @Transactional
    public RemarkResponse setEstimatedResolution(UUID id, SetEstimatedResolutionRequest request, UserContext actor) {
        Remark remark = getRemark(id);
        assertAssignedMaster(remark, actor);
        assertCanEstimateResolution(remark);

        LocalDateTime createdAt = LocalDateTime.ofInstant(remark.getCreatedAt(), BUSINESS_ZONE);
        if (!request.estimatedResolutionAt().isAfter(createdAt)) {
            throw new InvalidRemarkTransitionException("Estimated resolution date must be after remark creation date");
        }
        if (!request.estimatedResolutionAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new InvalidRemarkTransitionException("Estimated resolution date must be after current Minsk time");
        }

        EstimatedDuration estimatedDuration = calculateEstimatedDuration(createdAt, request.estimatedResolutionAt());
        remark.setEstimatedResolutionAt(request.estimatedResolutionAt());
        remark.setEstimatedResolutionDuration(estimatedDuration.duration());
        remark.setEstimatedResolutionUnit(estimatedDuration.unit());

        saveHistory(
                remark.getId(),
                remark.getStatus(),
                remark.getStatus(),
                actor.userId(),
                "Estimated resolution date set"
        );
        return RemarkMapper.toResponse(remarkRepository.save(remark));
    }

    @Transactional
    public RemarkResponse resolve(UUID id, ResolveRemarkRequest request, UserContext actor) {
        Remark remark = getRemark(id);
        assertAssignedMaster(remark, actor);
        assertStatus(remark, RemarkStatus.IN_PROGRESS);
        assertEstimatedResolutionSet(remark);

        remark.setResolutionMessage(request.resolutionMessage().trim());
        remark.setResolvedAt(Instant.now());
        transition(remark, RemarkStatus.RESOLVED, actor.userId(), request.resolutionMessage().trim());
        Remark saved = remarkRepository.save(remark);

        var creator = userServiceClient.getActiveUser(saved.getCreatedByUserId());
        eventPublisher.publishEvent(new RemarkResolvedApplicationEvent(
                saved.getId(),
                saved.getRemarkNumber(),
                saved.getBranchId(),
                saved.getCreatedByUserId(),
                creator.email(),
                saved.getObjectBuilding(),
                saved.getResolutionMessage(),
                actor.userId()
        ));

        return RemarkMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InternalRemarkResponse findByIdInternal(UUID id) {
        Remark remark = getRemark(id);
        return RemarkMapper.toInternalResponse(remark, findAssignedByUserId(id));
    }

    @Transactional
    public InternalRemarkResponse markReportSent(UUID id) {
        Remark remark = getRemark(id);
        remark.setReportSentAt(Instant.now());
        return RemarkMapper.toInternalResponse(remarkRepository.save(remark), findAssignedByUserId(id));
    }

    private UUID findAssignedByUserId(UUID remarkId) {
        return remarkStatusHistoryRepository.findByRemarkIdOrderByChangedAtAsc(remarkId).stream()
                .filter(history -> history.getToStatus() == RemarkStatus.ASSIGNED)
                .map(RemarkStatusHistory::getChangedByUserId)
                .findFirst()
                .orElse(null);
    }

    private Remark getRemark(UUID id) {
        return remarkRepository.findById(id)
                .orElseThrow(() -> new RemarkNotFoundException(id));
    }

    private void transition(Remark remark, RemarkStatus toStatus, UUID actorId, String comment) {
        RemarkStatus fromStatus = remark.getStatus();
        remark.setStatus(toStatus);
        saveHistory(remark.getId(), fromStatus, toStatus, actorId, comment);
    }

    private void saveHistory(
            UUID remarkId,
            RemarkStatus fromStatus,
            RemarkStatus toStatus,
            UUID actorId,
            String comment
    ) {
        RemarkStatusHistory history = new RemarkStatusHistory();
        history.setRemarkId(remarkId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedByUserId(actorId);
        history.setComment(comment);
        remarkStatusHistoryRepository.save(history);
    }

    private void assertCanCreate(UserContext actor) {
        if (!actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD, RoleCode.APPLICANT)) {
            throw new RemarkAccessDeniedException("User cannot create remarks");
        }
    }

    private void assertCanAssign(UserContext actor) {
        if (!actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD)) {
            throw new RemarkAccessDeniedException("User cannot assign masters");
        }
    }

    private void assertCanViewCalendar(UserContext actor) {
        if (!actor.hasRole(RoleCode.MASTER)) {
            throw new RemarkAccessDeniedException("Only master can view repair calendar");
        }
    }

    private void assertValidCalendarRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new InvalidRemarkTransitionException("Calendar range end must be after start");
        }
    }

    private void assertAssignedMaster(Remark remark, UserContext actor) {
        if (!actor.hasRole(RoleCode.MASTER)) {
            throw new RemarkAccessDeniedException("Only assigned master can perform this action");
        }
        if (remark.getAssignedMasterId() == null || !remark.getAssignedMasterId().equals(actor.userId())) {
            throw new RemarkAccessDeniedException("Remark is not assigned to this master");
        }
    }

    private void assertCanEstimateResolution(Remark remark) {
        if (remark.getStatus() != RemarkStatus.ASSIGNED && remark.getStatus() != RemarkStatus.IN_PROGRESS) {
            throw new InvalidRemarkTransitionException(
                    "Estimated resolution can be set only for assigned or in-progress remarks"
            );
        }
    }

    private void assertEstimatedResolutionSet(Remark remark) {
        if (remark.getEstimatedResolutionAt() == null) {
            throw new InvalidRemarkTransitionException("Estimated resolution date must be set by assigned master");
        }
    }

    private boolean overlapsCalendarRange(Remark remark, LocalDateTime rangeFrom, LocalDateTime rangeTo) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(remark.getCreatedAt(), BUSINESS_ZONE);
        LocalDateTime estimatedAt = remark.getEstimatedResolutionAt();
        return !estimatedAt.isBefore(rangeFrom) && createdAt.isBefore(rangeTo);
    }

    private List<RemarkCalendarEventResponse> buildCalendarEvents(
            Remark remark,
            LocalDate fromDate,
            LocalDate toDate,
            Map<CalendarEntryKey, RemarkCalendarEntry> entries
    ) {
        LocalDate createdDate = LocalDateTime.ofInstant(remark.getCreatedAt(), BUSINESS_ZONE).toLocalDate();
        LocalDate estimatedDate = remark.getEstimatedResolutionAt().toLocalDate();
        LocalDate firstDate = createdDate.isAfter(fromDate) ? createdDate : fromDate;
        LocalDate lastExclusive = estimatedDate.plusDays(1).isBefore(toDate) ? estimatedDate.plusDays(1) : toDate;

        return firstDate.datesUntil(lastExclusive)
                .flatMap(workDate -> {
                    RemarkCalendarEntry entry = entries.get(new CalendarEntryKey(remark.getId(), workDate));
                    if (entry != null && entry.isHidden()) {
                        return Stream.empty();
                    }
                    RemarkCalendarEventResponse response = entry == null
                            ? toCalendarEventResponse(
                                    remark,
                                    workDate,
                                    DEFAULT_WORK_START_TIME,
                                    DEFAULT_WORK_END_TIME,
                                    false
                            )
                            : toCalendarEventResponse(remark, entry);
                    return Stream.of(response);
                })
                .toList();
    }

    private RemarkCalendarEventResponse toCalendarEventResponse(Remark remark, RemarkCalendarEntry entry) {
        return toCalendarEventResponse(
                remark,
                entry.getWorkDate(),
                entry.getWorkStartTime(),
                entry.getWorkEndTime(),
                true
        );
    }

    private RemarkCalendarEventResponse toCalendarEventResponse(
            Remark remark,
            LocalDate workDate,
            LocalTime workStartTime,
            LocalTime workEndTime,
            boolean customTime
    ) {
        LocalDateTime start = LocalDateTime.of(workDate, workStartTime);
        LocalDateTime end = LocalDateTime.of(workDate, workEndTime);
        return new RemarkCalendarEventResponse(
                remark.getId(),
                remark.getRemarkNumber(),
                remark.getRemarkNumber() + " - " + remark.getObjectBuilding(),
                remark.getObjectBuilding(),
                remark.getStatus(),
                workDate,
                workStartTime,
                workEndTime,
                start,
                end,
                remark.getEstimatedResolutionAt(),
                customTime
        );
    }

    private void assertCalendarDateWithinResolutionPeriod(Remark remark, LocalDate workDate) {
        LocalDate createdDate = LocalDateTime.ofInstant(remark.getCreatedAt(), BUSINESS_ZONE).toLocalDate();
        LocalDate estimatedDate = remark.getEstimatedResolutionAt().toLocalDate();
        if (workDate.isBefore(createdDate) || workDate.isAfter(estimatedDate)) {
            throw new InvalidRemarkTransitionException("Calendar event date must be within repair period");
        }
    }

    private EstimatedDuration calculateEstimatedDuration(LocalDateTime discoveredAt, LocalDateTime estimatedAt) {
        long hours = Math.max(1, (Duration.between(discoveredAt, estimatedAt).toMinutes() + 59) / 60);
        if (hours % (24 * 7) == 0) {
            return new EstimatedDuration((int) (hours / (24 * 7)), EstimatedResolutionUnit.WEEKS);
        }
        if (hours % 24 == 0) {
            return new EstimatedDuration((int) (hours / 24), EstimatedResolutionUnit.DAYS);
        }
        return new EstimatedDuration((int) hours, EstimatedResolutionUnit.HOURS);
    }

    private void assertSameBranch(Remark remark, UserContext actor) {
        UUID branchId = actor.requireBranchId();
        if (!remark.getBranchId().equals(branchId)) {
            throw new RemarkAccessDeniedException("Remark belongs to another branch");
        }
    }

    private void assertCanView(Remark remark, UserContext actor) {
        if (!canView(remark, actor)) {
            throw new RemarkAccessDeniedException("User cannot view this remark");
        }
    }

    private boolean canView(Remark remark, UserContext actor) {
        if (actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD)) {
            return true;
        }
        if (actor.hasRole(RoleCode.APPLICANT)) {
            UUID branchId = actor.branchId();
            return branchId != null && remark.getBranchId().equals(branchId);
        }
        if (actor.hasRole(RoleCode.MASTER)) {
            return remark.getAssignedMasterId() != null
                    && remark.getAssignedMasterId().equals(actor.userId());
        }
        return false;
    }

    private void assertStatus(Remark remark, RemarkStatus expectedStatus) {
        if (remark.getStatus() != expectedStatus) {
            throw new InvalidRemarkTransitionException(
                    "Remark must be in status " + expectedStatus + ", but was " + remark.getStatus()
            );
        }
    }

    private record EstimatedDuration(int duration, EstimatedResolutionUnit unit) {
    }

    private record CalendarEntryKey(UUID remarkId, LocalDate workDate) {
    }

    private static String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static PagedRemarkResponse toPagedResponse(Page<Remark> page) {
        return new PagedRemarkResponse(
                page.getContent().stream().map(RemarkMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
