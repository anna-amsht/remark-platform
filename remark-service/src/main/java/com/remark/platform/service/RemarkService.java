package com.remark.platform.service;

import com.remark.platform.client.NotificationClient;
import com.remark.platform.client.ReportClient;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.dto.AssignMasterRequest;
import com.remark.platform.dto.CreateRemarkRequest;
import com.remark.platform.dto.RemarkResponse;
import com.remark.platform.dto.ResolveRemarkRequest;
import com.remark.platform.entity.Remark;
import com.remark.platform.entity.RemarkStatus;
import com.remark.platform.entity.RemarkStatusHistory;
import com.remark.platform.exceptions.InvalidRemarkTransitionException;
import com.remark.platform.exceptions.RemarkAccessDeniedException;
import com.remark.platform.exceptions.RemarkNotFoundException;
import com.remark.platform.repository.RemarkRepository;
import com.remark.platform.repository.RemarkStatusHistoryRepository;
import com.remark.platform.security.RoleCode;
import com.remark.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RemarkService {

    private final RemarkRepository remarkRepository;
    private final RemarkStatusHistoryRepository remarkStatusHistoryRepository;
    private final RemarkNumberGenerator remarkNumberGenerator;
    private final UserServiceClient userServiceClient;
    private final NotificationClient notificationClient;
    private final ReportClient reportClient;

    public RemarkService(
            RemarkRepository remarkRepository,
            RemarkStatusHistoryRepository remarkStatusHistoryRepository,
            RemarkNumberGenerator remarkNumberGenerator,
            UserServiceClient userServiceClient,
            NotificationClient notificationClient,
            ReportClient reportClient
    ) {
        this.remarkRepository = remarkRepository;
        this.remarkStatusHistoryRepository = remarkStatusHistoryRepository;
        this.remarkNumberGenerator = remarkNumberGenerator;
        this.userServiceClient = userServiceClient;
        this.notificationClient = notificationClient;
        this.reportClient = reportClient;
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
    public List<RemarkResponse> findAll(UserContext actor, RemarkStatus status, UUID branchIdFilter) {
        List<Remark> remarks;

        if (actor.hasRole(RoleCode.MASTER) && !actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD, RoleCode.APPLICANT)) {
            remarks = remarkRepository.findByAssignedMasterIdOrderByCreatedAtDesc(actor.userId());
        } else if (actor.hasRole(RoleCode.ADMIN)) {
            if (branchIdFilter != null) {
                remarks = status == null
                        ? remarkRepository.findByBranchIdOrderByCreatedAtDesc(branchIdFilter)
                        : remarkRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchIdFilter, status);
            } else {
                remarks = remarkRepository.findAllOrderByCreatedAtDesc();
            }
        } else {
            UUID branchId = actor.requireBranchId();
            remarks = status == null
                    ? remarkRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                    : remarkRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status);
        }

        return remarks.stream()
                .filter(remark -> canView(remark, actor))
                .map(RemarkMapper::toResponse)
                .toList();
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

        transition(remark, RemarkStatus.IN_PROGRESS, actor.userId(), "Work started");
        return RemarkMapper.toResponse(remarkRepository.save(remark));
    }

    @Transactional
    public RemarkResponse resolve(UUID id, ResolveRemarkRequest request, UserContext actor) {
        Remark remark = getRemark(id);
        assertAssignedMaster(remark, actor);
        assertStatus(remark, RemarkStatus.IN_PROGRESS);

        remark.setResolutionMessage(request.resolutionMessage().trim());
        remark.setResolvedAt(Instant.now());
        transition(remark, RemarkStatus.RESOLVED, actor.userId(), request.resolutionMessage().trim());
        Remark saved = remarkRepository.save(remark);

        var creator = userServiceClient.getActiveUser(saved.getCreatedByUserId());
        notificationClient.sendRemarkResolved(
                creator.email(),
                saved.getRemarkNumber(),
                saved.getObjectBuilding()
        );
        reportClient.requestReport(saved.getId());

        return RemarkMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InternalRemarkResponse findByIdInternal(UUID id) {
        return RemarkMapper.toInternalResponse(getRemark(id));
    }

    @Transactional
    public InternalRemarkResponse markReportSent(UUID id) {
        Remark remark = getRemark(id);
        remark.setReportSentAt(Instant.now());
        return RemarkMapper.toInternalResponse(remarkRepository.save(remark));
    }

    @Transactional
    public RemarkResponse close(UUID id, UserContext actor) {
        assertCanClose(actor);
        Remark remark = getRemark(id);
        assertSameBranch(remark, actor);
        assertStatus(remark, RemarkStatus.RESOLVED);

        remark.setClosedAt(Instant.now());
        transition(remark, RemarkStatus.CLOSED, actor.userId(), "Remark closed");
        return RemarkMapper.toResponse(remarkRepository.save(remark));
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

    private void assertCanClose(UserContext actor) {
        if (!actor.hasAnyRole(RoleCode.ADMIN, RoleCode.BRANCH_HEAD)) {
            throw new RemarkAccessDeniedException("User cannot close remarks");
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

    private void assertSameBranch(Remark remark, UserContext actor) {
        if (actor.hasRole(RoleCode.ADMIN)) {
            return;
        }
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
        if (actor.hasRole(RoleCode.ADMIN)) {
            return true;
        }
        if (actor.hasAnyRole(RoleCode.BRANCH_HEAD, RoleCode.APPLICANT)) {
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
}
