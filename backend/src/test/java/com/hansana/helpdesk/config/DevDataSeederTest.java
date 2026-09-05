package com.hansana.helpdesk.config;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DevDataSeeder}.
 *
 * <p>No database, no H2, no Testcontainers. All dependencies are mocked with Mockito.
 * Plaintext passwords are referenced only within this test class and are never logged.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevDataSeederTest {

    // Fixed development emails matching application.yml defaults
    private static final String ADMIN_EMAIL = "admin@helpdesk.dev";
    private static final String AGENT_EMAIL = "agent@helpdesk.dev";
    private static final String USER_EMAIL  = "user@helpdesk.dev";

    // Arbitrary test-only passwords — only used to verify the encoder is called
    private static final String ADMIN_PASSWORD = "AdminTestPass!1";
    private static final String AGENT_PASSWORD = "AgentTestPass!1";
    private static final String USER_PASSWORD  = "UserTestPass!1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Helper that builds a seeder instance with the given enabled flag and passwords
    private DevDataSeeder seeder(boolean enabled,
                                 String adminPw, String agentPw, String userPw) {
        return new DevDataSeeder(
                enabled,
                ADMIN_EMAIL, AGENT_EMAIL, USER_EMAIL,
                "Admin", "Dev",
                "Agent", "Dev",
                "User",  "Dev",
                adminPw, agentPw, userPw,
                userRepository, passwordEncoder
        );
    }

    // Convenience: all passwords supplied
    private DevDataSeeder enabledSeeder() {
        return seeder(true, ADMIN_PASSWORD, AGENT_PASSWORD, USER_PASSWORD);
    }

    @BeforeEach
    void setUp() {
        // Default: all three dev emails are absent from the DB
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(AGENT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        // Default: encoder returns a deterministic fake hash
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(inv -> "encoded:" + inv.getArgument(0));
    }

    // -----------------------------------------------------------------------
    // 1. Seeding disabled
    // -----------------------------------------------------------------------

    @Nested
    class WhenSeedingIsDisabled {

        @Test
        void noRepositoryWritesOccur() throws Exception {
            DevDataSeeder s = seeder(false, "", "", "");

            s.run();

            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // -----------------------------------------------------------------------
    // 2. Missing passwords → fast-fail
    // -----------------------------------------------------------------------

    @Nested
    class WhenPasswordsAreMissing {

        @Test
        void missingAdminPasswordThrowsWithVariableName() {
            DevDataSeeder s = seeder(true, "", AGENT_PASSWORD, USER_PASSWORD);
            IllegalStateException ex = assertThrows(IllegalStateException.class, s::validatePasswords);
            assertTrue(ex.getMessage().contains("DEV_ADMIN_PASSWORD"),
                    "Exception should name DEV_ADMIN_PASSWORD");
            // Must NOT reveal any actual password in the message
            assertNotEquals(ADMIN_PASSWORD, ex.getMessage());
        }

        @Test
        void missingAgentPasswordThrowsWithVariableName() {
            DevDataSeeder s = seeder(true, ADMIN_PASSWORD, "", USER_PASSWORD);
            IllegalStateException ex = assertThrows(IllegalStateException.class, s::validatePasswords);
            assertTrue(ex.getMessage().contains("DEV_AGENT_PASSWORD"),
                    "Exception should name DEV_AGENT_PASSWORD");
        }

        @Test
        void missingUserPasswordThrowsWithVariableName() {
            DevDataSeeder s = seeder(true, ADMIN_PASSWORD, AGENT_PASSWORD, "");
            IllegalStateException ex = assertThrows(IllegalStateException.class, s::validatePasswords);
            assertTrue(ex.getMessage().contains("DEV_USER_PASSWORD"),
                    "Exception should name DEV_USER_PASSWORD");
        }

        @Test
        void runThrowsWhenAdminPasswordMissing() {
            DevDataSeeder s = seeder(true, "", AGENT_PASSWORD, USER_PASSWORD);
            assertThrows(IllegalStateException.class, s::run);
            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 3. All three users absent → all three are created
    // -----------------------------------------------------------------------

    @Nested
    class WhenAllUsersAreAbsent {

        @Test
        void allThreeUsersAreCreated() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());
            List<User> saved = captor.getAllValues();

            // One saved user per role
            assertTrue(saved.stream().anyMatch(u -> u.getRole() == UserRole.ADMIN));
            assertTrue(saved.stream().anyMatch(u -> u.getRole() == UserRole.SUPPORT_AGENT));
            assertTrue(saved.stream().anyMatch(u -> u.getRole() == UserRole.USER));
        }

        @Test
        void adminUserHasCorrectAttributes() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());

            User admin = captor.getAllValues().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN)
                    .findFirst()
                    .orElseThrow();

            assertEquals(ADMIN_EMAIL, admin.getEmail());
            assertEquals("Admin", admin.getFirstName());
            assertEquals("Dev", admin.getLastName());
            assertTrue(admin.isActive());
        }

        @Test
        void agentUserHasCorrectAttributes() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());

            User agent = captor.getAllValues().stream()
                    .filter(u -> u.getRole() == UserRole.SUPPORT_AGENT)
                    .findFirst()
                    .orElseThrow();

            assertEquals(AGENT_EMAIL, agent.getEmail());
            assertEquals("Agent", agent.getFirstName());
            assertEquals("Dev", agent.getLastName());
            assertTrue(agent.isActive());
        }

        @Test
        void regularUserHasCorrectAttributes() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());

            User user = captor.getAllValues().stream()
                    .filter(u -> u.getRole() == UserRole.USER)
                    .findFirst()
                    .orElseThrow();

            assertEquals(USER_EMAIL, user.getEmail());
            assertEquals("User", user.getFirstName());
            assertEquals("Dev", user.getLastName());
            assertTrue(user.isActive());
        }

        @Test
        void passwordIsEncodedNotStoredPlaintext() throws Exception {
            // Use a real BCrypt encoder to verify actual encoding behaviour
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder real =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            DevDataSeeder s = new DevDataSeeder(
                    true,
                    ADMIN_EMAIL, AGENT_EMAIL, USER_EMAIL,
                    "Admin", "Dev", "Agent", "Dev", "User", "Dev",
                    ADMIN_PASSWORD, AGENT_PASSWORD, USER_PASSWORD,
                    userRepository, real
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            s.run();
            verify(userRepository, times(3)).save(captor.capture());

            for (User saved : captor.getAllValues()) {
                // Stored hash must not equal the plaintext
                String rawPw = rawPasswordFor(saved.getRole());
                assertNotEquals(rawPw, saved.getPassword(),
                        "Plaintext password must not be stored for role " + saved.getRole());
                // BCrypt hash must match the original plaintext
                assertTrue(real.matches(rawPw, saved.getPassword()),
                        "Stored hash must match plaintext for role " + saved.getRole());
            }
        }

        private String rawPasswordFor(UserRole role) {
            return switch (role) {
                case ADMIN -> ADMIN_PASSWORD;
                case SUPPORT_AGENT -> AGENT_PASSWORD;
                case USER -> USER_PASSWORD;
            };
        }
    }

    // -----------------------------------------------------------------------
    // 4. Existing users are skipped
    // -----------------------------------------------------------------------

    @Nested
    class WhenUsersAlreadyExist {

        @Test
        void existingAdminIsNotSavedAgain() throws Exception {
            User existing = existingUser(ADMIN_EMAIL, UserRole.ADMIN);
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existing));

            enabledSeeder().run();

            // agent and user should still be created (2 saves), but never admin
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(2)).save(captor.capture());
            assertTrue(captor.getAllValues().stream()
                    .noneMatch(u -> ADMIN_EMAIL.equals(u.getEmail())),
                    "Admin must not be saved when already present");
        }

        @Test
        void existingPasswordIsNotOverwritten() throws Exception {
            User existing = existingUser(AGENT_EMAIL, UserRole.SUPPORT_AGENT);
            existing.setPassword("original-encoded-password");
            when(userRepository.findByEmail(AGENT_EMAIL)).thenReturn(Optional.of(existing));

            enabledSeeder().run();

            // The existing user object should never be passed to save()
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(2)).save(captor.capture());
            captor.getAllValues().forEach(u ->
                assertNotEquals(AGENT_EMAIL, u.getEmail(),
                    "Existing agent must not be saved/overwritten"));
        }

        @Test
        void existingRoleIsNotOverwritten() throws Exception {
            // Suppose admin was manually demoted to USER in DB
            User existing = existingUser(ADMIN_EMAIL, UserRole.USER);
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existing));

            enabledSeeder().run();

            // Only 2 saves (agent + user); the demoted admin is left untouched
            verify(userRepository, times(2)).save(any());
        }

        @Test
        void allExistingUsersResultsInNoSaves() throws Exception {
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingUser(ADMIN_EMAIL, UserRole.ADMIN)));
            when(userRepository.findByEmail(AGENT_EMAIL)).thenReturn(Optional.of(existingUser(AGENT_EMAIL, UserRole.SUPPORT_AGENT)));
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser(USER_EMAIL, UserRole.USER)));

            enabledSeeder().run();

            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 5. Idempotency — running twice creates no duplicates
    // -----------------------------------------------------------------------

    @Nested
    class IdempotencyTests {

        @Test
        void runningTwiceDoesNotCreateDuplicates() throws Exception {
            DevDataSeeder s = enabledSeeder();

            // First run: all absent
            s.run();
            verify(userRepository, times(3)).save(any());

            // Second run: all now "exist"
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingUser(ADMIN_EMAIL, UserRole.ADMIN)));
            when(userRepository.findByEmail(AGENT_EMAIL)).thenReturn(Optional.of(existingUser(AGENT_EMAIL, UserRole.SUPPORT_AGENT)));
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser(USER_EMAIL, UserRole.USER)));

            s.run();

            // Total saves is still 3 (only from the first run)
            verify(userRepository, times(3)).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 6. Role mapping — correct role per email
    // -----------------------------------------------------------------------

    @Nested
    class RoleMappingTests {

        @Test
        void adminEmailMapsToAdminRole() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());

            User admin = captor.getAllValues().stream()
                    .filter(u -> ADMIN_EMAIL.equals(u.getEmail()))
                    .findFirst().orElseThrow();

            assertEquals(UserRole.ADMIN, admin.getRole());
        }

        @Test
        void agentEmailMapsToSupportAgentRole() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());

            User agent = captor.getAllValues().stream()
                    .filter(u -> AGENT_EMAIL.equals(u.getEmail()))
                    .findFirst().orElseThrow();

            assertEquals(UserRole.SUPPORT_AGENT, agent.getRole());
        }

        @Test
        void userEmailMapsToUserRole() throws Exception {
            enabledSeeder().run();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(3)).save(captor.capture());

            User user = captor.getAllValues().stream()
                    .filter(u -> USER_EMAIL.equals(u.getEmail()))
                    .findFirst().orElseThrow();

            assertEquals(UserRole.USER, user.getRole());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static User existingUser(String email, UserRole role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setFirstName("Existing");
        u.setLastName("User");
        u.setRole(role);
        u.setActive(true);
        u.setPassword("some-existing-hash");
        return u;
    }
}
