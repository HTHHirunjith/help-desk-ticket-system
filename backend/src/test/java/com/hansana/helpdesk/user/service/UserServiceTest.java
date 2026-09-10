package com.hansana.helpdesk.user.service;

import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.common.exception.EmailAlreadyExistsException;
import com.hansana.helpdesk.common.exception.EmailDeliveryException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, emailService, temporaryPasswordGenerator);
    }

    @Test
    void findUsers_withRoleAndActive_filtersCorrectly() {
        User agent = new User();
        agent.setId(UUID.randomUUID());
        agent.setFirstName("Agent");
        agent.setLastName("Dev");
        agent.setEmail("agent@helpdesk.dev");
        agent.setRole(UserRole.SUPPORT_AGENT);
        agent.setActive(true);

        when(userRepository.findByRoleAndActiveOrderByFirstNameAscLastNameAsc(UserRole.SUPPORT_AGENT, true))
                .thenReturn(List.of(agent));

        List<UserResponse> result = userService.findUsers(UserRole.SUPPORT_AGENT, true);

        assertEquals(1, result.size());
        assertEquals("Agent", result.get(0).getFirstName());
        assertEquals("agent@helpdesk.dev", result.get(0).getEmail());
        assertEquals(UserRole.SUPPORT_AGENT, result.get(0).getRole());
        verify(userRepository).findByRoleAndActiveOrderByFirstNameAscLastNameAsc(UserRole.SUPPORT_AGENT, true);
    }

    @Test
    void getUserById_whenExists_returnsUserResponse() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setRole(UserRole.USER);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    void getUserById_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(id));
    }

    @Test
    void createUser_withSupportAgentRole_generatesTempPassword_savesAndSendsEmail() {
        CreateUserRequest request = new CreateUserRequest("Agent", "One", "agent1@helpdesk.dev", UserRole.SUPPORT_AGENT);

        when(userRepository.findByEmail("agent1@helpdesk.dev")).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn("TempSecure@123");
        when(passwordEncoder.encode("TempSecure@123")).thenReturn("$2a$10$encodedTempPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        doNothing().when(emailService).sendTemporaryCredentialsEmail(any(), any(), any());

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("agent1@helpdesk.dev", response.getEmail());
        assertEquals(UserRole.SUPPORT_AGENT, response.getRole());
        assertTrue(response.isActive());
        assertTrue(response.isMustChangePassword());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("$2a$10$encodedTempPass", saved.getPassword());
        assertTrue(saved.isMustChangePassword());

        verify(emailService).sendTemporaryCredentialsEmail(
                eq("agent1@helpdesk.dev"),
                eq("Agent One"),
                eq("TempSecure@123")
        );
    }

    @Test
    void createUser_withAdminRole_generatesTempPassword_savesAndSendsEmail() {
        CreateUserRequest request = new CreateUserRequest("Super", "Admin", "admin2@helpdesk.dev", UserRole.ADMIN);

        when(userRepository.findByEmail("admin2@helpdesk.dev")).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn("AdminSecure@999");
        when(passwordEncoder.encode("AdminSecure@999")).thenReturn("$2a$10$encodedAdminPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("admin2@helpdesk.dev", response.getEmail());
        assertEquals(UserRole.ADMIN, response.getRole());
        assertTrue(response.isMustChangePassword());
        verify(emailService).sendTemporaryCredentialsEmail(eq("admin2@helpdesk.dev"), eq("Super Admin"), eq("AdminSecure@999"));
    }

    @Test
    void createUser_withUserRole_throwsIllegalArgumentException() {
        CreateUserRequest request = new CreateUserRequest("Normal", "User", "user@helpdesk.dev", UserRole.USER);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("Administrative account creation is only permitted"));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendTemporaryCredentialsEmail(any(), any(), any());
    }

    @Test
    void createUser_withDuplicateEmail_throwsEmailAlreadyExistsException() {
        CreateUserRequest request = new CreateUserRequest("Agent", "Dup", "dup@helpdesk.dev", UserRole.SUPPORT_AGENT);

        when(userRepository.findByEmail("dup@helpdesk.dev")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendTemporaryCredentialsEmail(any(), any(), any());
    }

    @Test
    void createUser_whenEmailSendingFails_propagatesException() {
        CreateUserRequest request = new CreateUserRequest("Agent", "Fail", "fail@helpdesk.dev", UserRole.SUPPORT_AGENT);

        when(userRepository.findByEmail("fail@helpdesk.dev")).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generate()).thenReturn("TempPass@123");
        when(passwordEncoder.encode("TempPass@123")).thenReturn("$2a$10$encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        doThrow(new EmailDeliveryException("SMTP error"))
                .when(emailService).sendTemporaryCredentialsEmail(any(), any(), any());

        assertThrows(EmailDeliveryException.class, () -> userService.createUser(request));
    }

    @Test
    void updateUser_withValidFields_updatesProfile() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);
        existing.setFirstName("OldFirst");
        existing.setLastName("OldLast");
        existing.setEmail("old@example.com");
        existing.setRole(UserRole.SUPPORT_AGENT);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(existing)).thenReturn(existing);

        UpdateUserRequest request = new UpdateUserRequest("NewFirst", "NewLast", "new@example.com");
        UserResponse response = userService.updateUser(id, request);

        assertEquals("NewFirst", existing.getFirstName());
        assertEquals("NewLast", existing.getLastName());
        assertEquals("new@example.com", existing.getEmail());
        assertEquals(UserRole.SUPPORT_AGENT, existing.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_withDuplicateNewEmail_throwsEmailAlreadyExistsException() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);
        existing.setEmail("current@example.com");

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("taken@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        UpdateUserRequest request = new UpdateUserRequest("NewFirst", "NewLast", "taken@example.com");

        assertThrows(EmailAlreadyExistsException.class, () -> userService.updateUser(id, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void activateUser_activatesSuccessfully() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setActive(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.activateUser(id);

        assertTrue(user.isActive());
        assertTrue(response.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_deactivatesSuccessfully() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail("target@helpdesk.dev");
        user.setRole(UserRole.SUPPORT_AGENT);
        user.setActive(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.deactivateUser(id, "admin@helpdesk.dev");

        assertFalse(user.isActive());
        assertFalse(response.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_whenAdminDeactivatesSelf_throwsSelfDeactivationException() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail("admin@helpdesk.dev");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        SelfDeactivationException ex = assertThrows(SelfDeactivationException.class, () ->
                userService.deactivateUser(id, "admin@helpdesk.dev"));

        assertEquals("Administrators cannot deactivate their own account", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateUser_whenLastActiveAdmin_throwsLastActiveAdminException() {
        UUID id = UUID.randomUUID();
        User targetAdmin = new User();
        targetAdmin.setId(id);
        targetAdmin.setEmail("otheradmin@helpdesk.dev");
        targetAdmin.setRole(UserRole.ADMIN);
        targetAdmin.setActive(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        LastActiveAdminException ex = assertThrows(LastActiveAdminException.class, () ->
                userService.deactivateUser(id, "currentadmin@helpdesk.dev"));

        assertEquals("Cannot deactivate the last active administrator", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
