package service;

import model.AttendanceLog;
import model.ContributionCalculator;
import model.Contributions;
import model.EarningsCalculator;
import model.Employee;
import model.OvertimePolicy;
import model.PayrollResult;
import model.TaxCalculator;
import model.TaxableBenefitsPolicy;
import model.WorkHoursCalculator;
import model.WorkHoursSummary;
import repository.AttendanceRepository;
import repository.EmployeeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DefaultPayrollService implements PayrollService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MAX_REASONABLE_LATE_MINUTES_PER_MONTH = 3000;
    private static final DateTimeFormatter TIME_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final EmployeeRepository employeeRepo;
    private final AttendanceRepository attendanceRepo;
    private final WorkHoursCalculator hoursCalculator;
    private final EarningsCalculator earningsCalculator;
    private final ContributionCalculator contributionCalculator;
    private final TaxCalculator taxCalculator;
    private final OvertimePolicy overtimePolicy;
    private final TaxableBenefitsPolicy benefitsPolicy;

    public DefaultPayrollService(
            EmployeeRepository employeeRepo,
            AttendanceRepository attendanceRepo,
            WorkHoursCalculator hoursCalculator,
            EarningsCalculator earningsCalculator,
            ContributionCalculator contributionCalculator,
            TaxCalculator taxCalculator,
            OvertimePolicy overtimePolicy,
            TaxableBenefitsPolicy benefitsPolicy
    ) {
        this.employeeRepo = Objects.requireNonNull(employeeRepo, "employeeRepo is required");
        this.attendanceRepo = Objects.requireNonNull(attendanceRepo, "attendanceRepo is required");
        this.hoursCalculator = Objects.requireNonNull(hoursCalculator, "hoursCalculator is required");
        this.earningsCalculator = Objects.requireNonNull(earningsCalculator, "earningsCalculator is required");
        this.contributionCalculator = Objects.requireNonNull(contributionCalculator, "contributionCalculator is required");
        this.taxCalculator = Objects.requireNonNull(taxCalculator, "taxCalculator is required");
        this.overtimePolicy = Objects.requireNonNull(overtimePolicy, "overtimePolicy is required");
        this.benefitsPolicy = Objects.requireNonNull(benefitsPolicy, "benefitsPolicy is required");
    }

    @Override
    public PayrollResultDto computeForMonthDto(String employeeId, int year, int month) {
        LocalDate start = YearMonth.of(year, month).atDay(1);
        LocalDate end = YearMonth.of(year, month).atEndOfMonth();
        PayrollComputationData data = computePayrollData(employeeId, start, end);
        PayrollResult result = data.result();
        Contributions contributions = result.getContributions();

        return new PayrollResultDto(
                result.getEmployeeId(),
                scale2(result.getHoursWorked()).toPlainString(),
                scale2(result.getOvertimeHours()).toPlainString(),
                result.getLateMinutes(),
                scale2(result.getBasicPay()).toPlainString(),
                scale2(result.getAllowances()).toPlainString(),
                scale2(result.getOvertimePay()).toPlainString(),
                scale2(result.getLateDeduction()).toPlainString(),
                scale2(result.getGrossPay()).toPlainString(),
                contributions == null ? "0.00" : scale2(contributions.getSss()).toPlainString(),
                contributions == null ? "0.00" : scale2(contributions.getPhilHealth()).toPlainString(),
                contributions == null ? "0.00" : scale2(contributions.getPagIbig()).toPlainString(),
                scale2(result.getWithholdingTax()).toPlainString(),
                scale2(result.getNetPay()).toPlainString(),
                data.attendanceEntries().size(),
                data.attendanceEntries()
        );
    }

    public PayrollResult computeDomainForDateRange(String employeeNumber, LocalDate start, LocalDate end) {
        return computePayrollData(employeeNumber, start, end).result();
    }

    @Override
    public PayrollResult computeForDateRange(String employeeNumber, LocalDate start, LocalDate end) {
        return computeDomainForDateRange(employeeNumber, start, end);
    }

    @Override
    public PayrollResult computeForMonth(String employeeNumber, int year, int month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        return computeDomainForDateRange(
                employeeNumber,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    private PayrollComputationData computePayrollData(String employeeNumber, LocalDate start, LocalDate end) {
        String employeeId = normalizeEmployeeNumber(employeeNumber);
        validateDateRange(start, end);

        employeeRepo.load();
        attendanceRepo.load();

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        validateEmployeeCompensation(employee);

        List<AttendanceLog> logs = getAttendanceLogs(employeeId, start, end);
        WorkHoursSummary summary = safeSummary(hoursCalculator.summarize(logs));

        BigDecimal hoursWorked = scale2(summary.getTotalHoursWorked());
        BigDecimal overtimeHours = scale2(summary.getOvertimeHoursWorked());
        int lateMinutes = sanitizeLateMinutes(summary.getTotalLateMinutes());

        BigDecimal hourlyRate = nz(employee.getHourlyRate());
        BigDecimal basicSalary = nz(employee.getBasicSalary());

        BigDecimal basicPay = scale2(
                nz(earningsCalculator.computeBasicPay(hourlyRate, hoursWorked))
        );

        BigDecimal allowances = scale2(
                nz(earningsCalculator.computeAllowances(employee))
        );

        BigDecimal overtimePay = scale2(
                nz(earningsCalculator.computeOvertimePay(hourlyRate, overtimeHours, overtimePolicy))
        );

        BigDecimal grossBeforeLate = scale2(
                nz(earningsCalculator.computeGrossPay(basicPay, allowances, overtimePay))
        );

        BigDecimal lateDeduction = scale2(
                computeLateDeduction(hourlyRate, lateMinutes)
        );

        BigDecimal grossPay = scale2(nonNegative(grossBeforeLate.subtract(lateDeduction)));

        Contributions contributions = safeContributions(
                contributionCalculator.computeAll(basicSalary)
        );

        BigDecimal taxableIncome = scale2(
                nz(taxCalculator.computeTaxableIncome(grossPay, contributions, benefitsPolicy))
        );

        BigDecimal withholdingTax = scale2(
                nz(taxCalculator.computeWithholdingTax(taxableIncome))
        );

        BigDecimal netPay = scale2(
                nz(taxCalculator.computeNetPay(grossPay, contributions, withholdingTax))
        );

        PayrollResult result = new PayrollResult(
                employeeId,
                hoursWorked,
                overtimeHours,
                lateMinutes,
                basicPay,
                allowances,
                overtimePay,
                lateDeduction,
                grossPay,
                contributions,
                withholdingTax,
                netPay
        );

        return new PayrollComputationData(result, buildAttendanceEntries(logs));
    }

    private List<AttendanceLog> getAttendanceLogs(String employeeId, LocalDate start, LocalDate end) {
        List<AttendanceLog> logs = attendanceRepo.findByEmployeeAndDateRange(employeeId, start, end);
        if (logs == null || logs.isEmpty()) {
            throw new IllegalArgumentException("No attendance records found for the selected payroll period.");
        }

        logs.sort(Comparator.comparing(AttendanceLog::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return logs;
    }

    private List<AttendanceEntryDto> buildAttendanceEntries(List<AttendanceLog> logs) {
        List<AttendanceEntryDto> entries = new ArrayList<>();

        for (AttendanceLog log : logs) {
            if (log == null) {
                continue;
            }

            int workedMinutes = Math.max(hoursCalculator.computeMinutesWorked(log), 0);
            int lateMinutes = Math.max(hoursCalculator.computeLateMinutes(log), 0);
            int overtimeMinutes = Math.max(hoursCalculator.computeOvertimeMinutes(log), 0);

            entries.add(new AttendanceEntryDto(
                    log.getDate() == null ? "" : log.getDate().toString(),
                    formatTime(log.getTimeIn()),
                    formatTime(log.getTimeOut()),
                    scale2(toHours(workedMinutes)).toPlainString(),
                    lateMinutes,
                    overtimeMinutes
            ));
        }

        return entries;
    }

    private BigDecimal toHours(int minutes) {
        return BigDecimal.valueOf(Math.max(minutes, 0))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private String formatTime(LocalTime value) {
        return value == null ? "-" : value.format(TIME_DISPLAY_FORMAT);
    }

    private void validateEmployeeCompensation(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee is required for payroll computation.");
        }

        validateCeiling(employee.getBasicSalary(), EmployeeValidator.MAX_BASIC_SALARY, "Basic Salary");
        validateCeiling(employee.getRiceSubsidy(), EmployeeValidator.MAX_ALLOWANCE, "Rice Subsidy");
        validateCeiling(employee.getPhoneAllowance(), EmployeeValidator.MAX_ALLOWANCE, "Phone Allowance");
        validateCeiling(employee.getClothingAllowance(), EmployeeValidator.MAX_ALLOWANCE, "Clothing Allowance");
        validateCeiling(employee.getSemiMonthlyRate(), EmployeeValidator.MAX_GROSS_SEMI_MONTHLY_RATE, "Gross Semi-monthly Rate");
        validateCeiling(employee.getHourlyRate(), EmployeeValidator.MAX_HOURLY_RATE, "Hourly Rate");
    }

    private void validateCeiling(BigDecimal value, BigDecimal maximum, String fieldName) {
        if (value == null || maximum == null) {
            return;
        }

        if (value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(fieldName + " exceeds the allowed ceiling.");
        }
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        String value = employeeNumber == null ? "" : employeeNumber.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Employee number is required");
        }
        return value;
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date");
        }
    }

    private WorkHoursSummary safeSummary(WorkHoursSummary summary) {
        return summary == null ? new WorkHoursSummary(0, 0, 0) : summary;
    }

    private Contributions safeContributions(Contributions contributions) {
        return contributions == null ? new Contributions(ZERO, ZERO, ZERO) : contributions;
    }

    private int sanitizeLateMinutes(int lateMinutes) {
        if (lateMinutes < 0) {
            return 0;
        }
        if (lateMinutes > MAX_REASONABLE_LATE_MINUTES_PER_MONTH) {
            return 0;
        }
        return lateMinutes;
    }

    private BigDecimal computeLateDeduction(BigDecimal hourlyRate, int lateMinutes) {
        if (hourlyRate == null || hourlyRate.compareTo(ZERO) <= 0 || lateMinutes <= 0) {
            return ZERO;
        }

        BigDecimal perMinuteRate = hourlyRate.divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        return perMinuteRate
                .multiply(BigDecimal.valueOf(lateMinutes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return nz(value).compareTo(ZERO) < 0 ? ZERO : value;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal scale2(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record PayrollComputationData(PayrollResult result, List<AttendanceEntryDto> attendanceEntries) {
    }
}
