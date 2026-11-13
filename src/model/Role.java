package model;

public enum Role {
    EMPLOYEE,
    HRADMIN,
    IT,
    HR,
    FINANCE;

    public static Role from(String s) {
        if (s == null) {
            return EMPLOYEE;
        }

        try {
            return Role.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EMPLOYEE;
        }
    }

    public boolean canAccessEmployeeDirectory() {
        return switch (this) {
            case EMPLOYEE, HRADMIN, HR, FINANCE -> true;
            default -> false;
        };
    }

    public boolean canViewAllEmployees() {
        return switch (this) {
            case HRADMIN, HR, FINANCE -> true;
            default -> false;
        };
    }

    public boolean canCreateEmployees() {
        return this == HRADMIN || this == HR;
    }

    public boolean canDeleteEmployees() {
        return this == HRADMIN || this == HR;
    }

    public boolean canEditGeneralEmployeeInfo() {
        return this == HRADMIN || this == HR;
    }

    public boolean canEditPayrollInfo() {
        return this == HRADMIN || this == FINANCE;
    }

    public boolean canApproveLeaves() {
        return this == HRADMIN || this == HR;
    }

    public boolean canManageUserAccounts() {
        return this == IT;
    }

    public boolean canViewEmployeeRecord(String targetEmployeeId, String currentEmployeeId) {
        if (canViewAllEmployees()) {
            return true;
        }

        return this == EMPLOYEE && sameEmployee(targetEmployeeId, currentEmployeeId);
    }

    public boolean canViewPayrollFor(String targetEmployeeId, String currentEmployeeId) {
        return canEditPayrollInfo() || sameEmployee(targetEmployeeId, currentEmployeeId);
    }

    private boolean sameEmployee(String left, String right) {
        return safe(left).equals(safe(right));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
