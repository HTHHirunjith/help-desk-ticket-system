package com.hansana.helpdesk.user.service;

import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.common.exception.EmailAlreadyExistsException;
import com.hansana.helpdesk.common.exception.LastActiveAdminException;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.common.exception.SelfDeactivationException;
import com.hansana.helpdesk.common.service.EmailService;
import com.hansana.helpdesk.common.util.TemporaryPasswordGenerator;
import com.hansana.helpdesk.user.dto.CreateUserRequest;
import com.hansana.helpdesk.user.dto.UpdateUserRequest;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       TemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
    }

    public List<UserResponse> findUsers(UserRole role, Boolean active) {
        List<User> users;
        if (role != null && active != null) {
            users = userRepository.findByRoleAndActiveOrderByFirstNameAscLastNameAsc(role, active);
        } else if (role != null) {
            users = userRepository.findByRoleOrderByFirstNameAscLastNameAsc(role);
        } else if (active != null) {
            users = userRepository.findByActiveOrderByFirstNameAscLastNameAsc(active);
        } else {
            users = userRepository.findAllByOrderByFirstNameAscLastNameAsc();
        }

        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromUser(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.getRole() == UserRole.USER) {
            throw new IllegalArgumentException("Administrative account creation is only permitted for SUPPORT_AGENT and ADMIN roles");
        }

        String email = request.getEmail().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        String hashedPassword = passwordEncoder.encode(temporaryPassword);

        User user = new User();
        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPassword(hashedPassword);
        user.setRole(request.getRole());
        user.setActive(true);
        user.setMustChangePassword(true);

        User savedUser = userRepository.save(user);

        // Deliver temporary credentials via email. If this fails, EmailDeliveryException will roll back the transaction.
        String fullName = savedUser.getFirstName() + " " + savedUser.getLastName();
        emailService.sendTemporaryCredentialsEmail(savedUser.getEmail(), fullName, temporaryPassword);

        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                Optional<User> existing = userRepository.findByEmail(newEmail);
                if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                    throw new EmailAlreadyExistsException("Email is already registered");
                }
                user.setEmail(newEmail);
            }
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }

    @Transactional
    public UserResponse activateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setActive(true);
        User savedUser = userRepository.save(user);
        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse deactivateUser(UUID id, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (currentAdminEmail != null && user.getEmail().equalsIgnoreCase(currentAdminEmail.trim())) {
            throw new SelfDeactivationException("Administrators cannot deactivate their own account");
        }

        if (user.getRole() == UserRole.ADMIN && user.isActive()) {
            long activeAdminCount = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
            if (activeAdminCount <= 1) {
                throw new LastActiveAdminException("Cannot deactivate the last active administrator");
            }
        }

        user.setActive(false);
        User savedUser = userRepository.save(user);
        return UserResponse.fromUser(savedUser);
    }
}
