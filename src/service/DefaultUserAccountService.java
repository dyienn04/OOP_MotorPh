package service;

import model.Employee;
import model.Role;
import model.UserAccount;
import repository.CredentialRepository;
import repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultUserAccountService implements UserAccountService {

    private final CredentialRepository credentialRepository;
    private final EmployeeRepository employeeRepository;

    public DefaultUserAccountService(
            CredentialRepository credentialRepository,
            EmployeeRepository employeeRepository
    ) {
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "credentialRepository is required");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository is required");
    }

    @Override
    public List<UserAccountDto> findAll() {
        credentialRepository.load();
        employeeRepository.load();

        List<UserAccount> accounts = credentialRepository.findAll();
        List<UserAccountDto> dtos = new ArrayList<>();

        if (accounts == null) {
            return dtos;
        }

        for (UserAccount account : accounts) {
            dtos.add(toDto(account));
        }

        return dtos;
    }

    @Override
    public UserAccountDto findByUsername(String username) {
        credentialRepository.load();
        employeeRepository.load();

        UserAccount account = credentialRepository.findByUsername(safe(username));
        return account == null ? null : toDto(account);
    }

    @Override
    public boolean add(UserAccountCreateRequest request) {
        if (request == null) {
            return false;
        }

        String username = safe(request.getUsername());
        String password = safe(request.getPassword());
        String employeeNumber = safe(request.getEmployeeNumber());

        if (username.isEmpty() || password.isEmpty() || employeeNumber.isEmpty()) {
            return false;
        }

        credentialRepository.load();
        employeeRepository.load();

        if (credentialRepository.findByUsername(username) != null) {
            return false;
        }

        if (employeeRepository.findById(employeeNumber).isEmpty()) {
            return false;
        }

        return credentialRepository.add(
                username,
                password,
                Role.from(safe(request.getRole())),
                employeeNumber
        );
    }

    @Override
    public boolean update(UserAccountUpdateRequest request) {
        if (request == null || safe(request.getUsername()).isEmpty()) {
            return false;
        }

        credentialRepository.load();
        employeeRepository.load();

        boolean changed = false;
        boolean ok = true;

        if (!safe(request.getNewPassword()).isEmpty()) {
            ok &= credentialRepository.updatePassword(
                    safe(request.getUsername()),
                    safe(request.getNewPassword())
            );
            changed = true;
        }

        if (!safe(request.getNewRole()).isEmpty()) {
            ok &= credentialRepository.updateRole(
                    safe(request.getUsername()),
                    Role.from(safe(request.getNewRole()))
            );
            changed = true;
        }

        if (!safe(request.getNewEmployeeNumber()).isEmpty()) {
            if (employeeRepository.findById(safe(request.getNewEmployeeNumber())).isEmpty()) {
                return false;
            }

            ok &= credentialRepository.updateEmployeeNo(
                    safe(request.getUsername()),
                    safe(request.getNewEmployeeNumber())
            );
            changed = true;
        }

        return changed && ok;
    }

    @Override
    public boolean deleteByUsername(String username) {
        return credentialRepository.delete(safe(username));
    }

    private UserAccountDto toDto(UserAccount account) {
        String employeeNo = safe(account.getEmployeeNumber());
        Employee employee = employeeRepository.findById(employeeNo).orElse(null);

        String firstName = employee == null ? "(Missing Employee Record)" : safe(employee.getFirstName());
        String position = employee == null ? "(Missing Employee Record)" : safe(employee.getPosition());

        return new UserAccountDto(
                safe(account.getCredentialId()),
                safe(account.getUsername()),
                account.getRole() == null ? "" : account.getRole().name(),
                employeeNo,
                firstName.isEmpty() ? "(Unknown)" : firstName,
                position.isEmpty() ? "(Unknown)" : position
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
