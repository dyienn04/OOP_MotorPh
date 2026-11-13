Credentials:
ADMIN ROLES
admin01	admin123

HR ROLES
admin07	1234

EMPLOYEE ROLES
emp01	1234

FINANCE ROLES
finance01	1234

IT ROLES
it01	1234

This revision addresses the issues raised in the feedback review.

Fixed issues

1. Attendance and payroll traceability
- Payroll computation now exposes attendance-backed details.
- The payroll view shows the processed days, time-in, time-out, worked hours, late minutes, and overtime minutes used in the computation.
- `PayrollService.computeForMonth()` and `computeForDateRange()` are now fully implemented.
- Payroll computation now fails fast when no attendance records exist for the requested payroll period.

2. Payroll role separation and finance access
- Added a new `FINANCE` role.
- Added a seeded Finance account:
  - Username: `finance01`
  - Password: `1234`
  - Employee #: `10011`
- Compensation and payroll-related fields are now editable by `FINANCE` and `HRADMIN`.
- HR can still manage general employee information, but payroll fields are read-only for HR.

3. Salary ceiling validation
- Added validation ceilings for:
  - Basic Salary
  - Allowances
  - Gross Semi-monthly Rate
  - Hourly Rate
- Added decimal precision checks and sanity checks to prevent absurd payroll values.
- Payroll processing also validates stored compensation values before computation.

4. Leave request validation
- Added maximum leave duration per request.
- Added validation against filing leave too far in advance.
- Added validation against filing leave in the past.
- Added overlap detection for pending/approved leave requests.
- Added employee ownership validation so employees can only file leave for themselves.
- Added approver-role validation for leave approval/rejection.

5. Role-based access control in the UI
- Regular employees now only see their own employee row/profile.
- Regular employees cannot browse the full employee list anymore.
- Payroll details are restricted to the employee themself, Finance, and HR Admin.
- User account management remains restricted to IT.

6. Ghost employee / orphan credential fix
- Login now rejects credentials whose linked employee record no longer exists.
- Employee deletion now cascades to the linked credential record.
- User account creation/update now requires a valid employee record.
- Removed the orphan seeded credential that pointed to employee `10034` with no employee master record.

Technical notes
- NetBeans project source/target version was adjusted from Java 24 to Java 21 for compatibility with the current toolchain.
- The code compiles successfully with Ant.
- Smoke tests were run against the key review findings.
