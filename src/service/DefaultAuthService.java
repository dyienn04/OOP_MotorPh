package service;

import model.Employee;
import model.UserAccount;
import repository.CredentialRepository;
import repository.EmployeeRepository;

import java.util.Objects;

public class DefaultAuthService implements AuthService {

    private final CredentialRepository credentialRepository;
    private final EmployeeRepository employeeRepository;

    public DefaultAuthService(
            CredentialRepository credentialRepository,
            EmployeeRepository employeeRepository
    ) {
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "credentialRepository is required");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository is required");
    }

    @Override
    public AuthenticatedUser login(String username, String password) {
        String cleanUsername = safe(username);
        String cleanPassword = safe(password);

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        credentialRepository.load();
        employeeRepository.load();

        UserAccount account = credentialRepository.validate(cleanUsername, cleanPassword);
        if (account == null || safe(account.getEmployeeNumber()).isEmpty()) {
            return null;
        }

        Employee employee = employeeRepository.findById(account.getEmployeeNumber()).orElse(null);
        if (employee == null) {
            return null;
        }

        return new AuthenticatedUser(
                safe(account.getCredentialId()),
                safe(account.getUsername()),
                account.getRole(),
                safe(account.getEmployeeNumber()),
                safeOrDefault(employee.getFirstName(), "(Unknown)"),
                safeOrDefault(employee.getPosition(), "(Unknown)")
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeOrDefault(String value, String fallback) {
        String clean = safe(value);
        return clean.isEmpty() ? fallback : clean;
    }
}
