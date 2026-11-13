package service;

import model.LeaveRequest;
import model.LeaveStatus;
import model.LeaveType;
import repository.EmployeeRepository;
import repository.LeaveRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DefaultLeaveService implements LeaveService {

    private static final long MAX_CONSECUTIVE_LEAVE_DAYS = 30;
    private static final long MAX_ADVANCE_FILING_DAYS = 365;
    private static final int MAX_REASON_LENGTH = 255;

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;

    public DefaultLeaveService(LeaveRepository leaveRepository, EmployeeRepository employeeRepository) {
        this.leaveRepository = Objects.requireNonNull(leaveRepository, "leaveRepository is required");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository is required");
    }

    @Override
    public boolean fileLeaveRequest(AuthenticatedUser currentUser, LeaveCreateRequest request) {
        validateLeaveRequest(currentUser, request);

        LeaveRequest leaveRequest = new LeaveRequest(
                UUID.randomUUID().toString(),
                safe(request.getEmployeeNumber()),
                safe(request.getEmployeeName()),
                toLeaveType(request.getLeaveType()),
                request.getStartDate(),
                request.getEndDate(),
                safe(request.getReason()),
                LeaveStatus.PENDING,
                LocalDateTime.now(),
                "",
                null
        );

        leaveRepository.create(leaveRequest);
        return true;
    }

    @Override
    public List<LeaveRequestDto> findMyLeaves(String employeeNumber) {
        List<LeaveRequest> requests = leaveRepository.findByEmployeeId(safe(employeeNumber));
        List<LeaveRequestDto> dtos = new ArrayList<>();

        if (requests == null) {
            return dtos;
        }

        requests.sort(Comparator.comparing(LeaveRequest::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        for (LeaveRequest request : requests) {
            dtos.add(toDto(request));
        }

        return dtos;
    }

    @Override
    public List<LeaveRequestDto> findPendingLeaves() {
        List<LeaveRequest> requests = leaveRepository.findByStatus(LeaveStatus.PENDING);
        List<LeaveRequestDto> dtos = new ArrayList<>();

        if (requests == null) {
            return dtos;
        }

        requests.sort(Comparator.comparing(LeaveRequest::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        for (LeaveRequest request : requests) {
            dtos.add(toDto(request));
        }

        return dtos;
    }

    @Override
    public LeaveRequestDto findLeaveById(String leaveId) {
        LeaveRequest request = leaveRepository.findById(safe(leaveId)).orElse(null);
        return request == null ? null : toDto(request);
    }

    @Override
    public boolean approveLeave(String leaveId, AuthenticatedUser approver) {
        return reviewLeave(leaveId, approver, true);
    }

    @Override
    public boolean rejectLeave(String leaveId, AuthenticatedUser approver) {
        return reviewLeave(leaveId, approver, false);
    }

    private boolean reviewLeave(String leaveId, AuthenticatedUser approver, boolean approve) {
        if (approver == null || approver.getRole() == null || !approver.getRole().canApproveLeaves()) {
            return false;
        }

        LeaveRequest request = leaveRepository.findById(safe(leaveId)).orElse(null);
        if (request == null || request.getStatus() != LeaveStatus.PENDING) {
            return false;
        }

        if (safe(request.getEmployeeId()).equals(safe(approver.getEmployeeNumber()))) {
            return false;
        }

        if (approve) {
            request.approve(safe(approver.getEmployeeNumber()));
        } else {
            request.deny(safe(approver.getEmployeeNumber()));
        }

        leaveRepository.update(request);
        return true;
    }

    private void validateLeaveRequest(AuthenticatedUser currentUser, LeaveCreateRequest request) {
        if (currentUser == null || request == null) {
            throw new IllegalArgumentException("Leave request is required.");
        }

        if (currentUser.getRole() == null) {
            throw new IllegalArgumentException("Authenticated user role is required.");
        }

        String employeeNumber = safe(request.getEmployeeNumber());
        String currentEmployeeNumber = safe(currentUser.getEmployeeNumber());
        String reason = safe(request.getReason());
        LeaveType leaveType = toLeaveType(request.getLeaveType());

        if (employeeNumber.isEmpty() || request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Employee, start date, and end date are required.");
        }

        if (!employeeNumber.equals(currentEmployeeNumber)) {
            throw new IllegalArgumentException("Employees can only file leave for their own account.");
        }

        employeeRepository.load();
        if (employeeRepository.findById(employeeNumber).isEmpty()) {
            throw new IllegalArgumentException("Employee record not found.");
        }

        if (leaveType == LeaveType.OTHER && safe(request.getLeaveType()).isBlank()) {
            throw new IllegalArgumentException("Leave type is required.");
        }

        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Reason is required.");
        }

        if (reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Reason must not exceed " + MAX_REASON_LENGTH + " characters.");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }

        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Leave start date cannot be in the past.");
        }

        if (request.getStartDate().isAfter(today.plusDays(MAX_ADVANCE_FILING_DAYS))) {
            throw new IllegalArgumentException("Leave can only be filed up to " + MAX_ADVANCE_FILING_DAYS + " days in advance.");
        }

        long requestedDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (requestedDays > MAX_CONSECUTIVE_LEAVE_DAYS) {
            throw new IllegalArgumentException(
                    "Leave request exceeds the maximum of " + MAX_CONSECUTIVE_LEAVE_DAYS + " consecutive days."
            );
        }

        ensureNoOverlap(employeeNumber, request.getStartDate(), request.getEndDate());
    }

    private void ensureNoOverlap(String employeeNumber, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> existingLeaves = leaveRepository.findByEmployeeId(employeeNumber);
        if (existingLeaves == null) {
            return;
        }

        for (LeaveRequest existing : existingLeaves) {
            if (existing == null || existing.getStatus() == LeaveStatus.DENIED) {
                continue;
            }

            if (datesOverlap(startDate, endDate, existing.getStartDate(), existing.getEndDate())) {
                throw new IllegalArgumentException("Leave request overlaps with an existing pending or approved leave.");
            }
        }
    }

    private boolean datesOverlap(LocalDate leftStart, LocalDate leftEnd, LocalDate rightStart, LocalDate rightEnd) {
        if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) {
            return false;
        }

        return !leftEnd.isBefore(rightStart) && !rightEnd.isBefore(leftStart);
    }

    private LeaveRequestDto toDto(LeaveRequest request) {
        return new LeaveRequestDto(
                safe(request.getLeaveId()),
                safe(request.getEmployeeId()),
                safe(request.getEmployeeName()),
                request.getLeaveType() == null ? "" : request.getLeaveType().name(),
                request.getStartDate() == null ? "" : request.getStartDate().toString(),
                request.getEndDate() == null ? "" : request.getEndDate().toString(),
                buildDateRangeDisplay(request),
                safe(request.getReason()),
                request.getStatus() == null ? "" : request.getStatus().name(),
                request.getRequestedAt() == null ? "" : request.getRequestedAt().toString(),
                safe(request.getReviewedBy()),
                request.getReviewedAt() == null ? "" : request.getReviewedAt().toString()
        );
    }

    private String buildDateRangeDisplay(LeaveRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            return "";
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        String dayLabel = days == 1 ? "day" : "days";

        return request.getStartDate() + " -> " + request.getEndDate() + " (" + days + " " + dayLabel + ")";
    }

    private LeaveType toLeaveType(String value) {
        String normalized = safe(value).toUpperCase().replace(" ", "_");
        try {
            return LeaveType.valueOf(normalized);
        } catch (Exception ex) {
            return LeaveType.OTHER;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
