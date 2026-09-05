package com.hansana.helpdesk.config;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds well-known development users on application startup.
 *
 * <p>Only runs when {@code app.seed.enabled=true}. Passwords must be supplied
 * via environment variables ({@code DEV_ADMIN_PASSWORD}, {@code DEV_AGENT_PASSWORD},
 * {@code DEV_USER_PASSWORD}). Missing passwords cause a fast-fail with a clear
 * error message naming only the missing variable.
 *
 * <p>The seeder is idempotent: existing users are never modified.
 *
 * <p><strong>No passwords, hashes, or secrets are written to any log.</strong>
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final boolean seedEnabled;

    private final String adminEmail;
    private final String agentEmail;
    private final String userEmail;

    private final String adminFirstName;
    private final String adminLastName;
    private final String agentFirstName;
    private final String agentLastName;
    private final String userFirstName;
    private final String userLastName;

    private final String adminPassword;
    private final String agentPassword;
    private final String userPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            @Value("${app.seed.enabled:false}") boolean seedEnabled,
            @Value("${app.seed.admin-email:admin@helpdesk.dev}") String adminEmail,
            @Value("${app.seed.agent-email:agent@helpdesk.dev}") String agentEmail,
            @Value("${app.seed.user-email:user@helpdesk.dev}") String userEmail,
            @Value("${app.seed.admin-first-name:Admin}") String adminFirstName,
            @Value("${app.seed.admin-last-name:Dev}") String adminLastName,
            @Value("${app.seed.agent-first-name:Agent}") String agentFirstName,
            @Value("${app.seed.agent-last-name:Dev}") String agentLastName,
            @Value("${app.seed.user-first-name:User}") String userFirstName,
            @Value("${app.seed.user-last-name:Dev}") String userLastName,
            @Value("${app.seed.admin-password:}") String adminPassword,
            @Value("${app.seed.agent-password:}") String agentPassword,
            @Value("${app.seed.user-password:}") String userPassword,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.seedEnabled = seedEnabled;
        this.adminEmail = adminEmail;
        this.agentEmail = agentEmail;
        this.userEmail = userEmail;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
        this.agentFirstName = agentFirstName;
        this.agentLastName = agentLastName;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
        this.adminPassword = adminPassword;
        this.agentPassword = agentPassword;
        this.userPassword = userPassword;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Development user seeding is disabled (SEED_ENABLED=false).");
            return;
        }

        log.info("Development user seeding is enabled.");

        validatePasswords();

        // Ordered map: email -> DevUserSpec
        Map<String, DevUserSpec> specs = buildSpecs();

        int created = 0;
        int skipped = 0;

        for (Map.Entry<String, DevUserSpec> entry : specs.entrySet()) {
            String email = entry.getKey();
            DevUserSpec spec = entry.getValue();

            if (userRepository.findByEmail(email).isPresent()) {
                log.info("Development user already exists, skipping: {}", email);
                skipped++;
            } else {
                User user = new User();
                user.setEmail(email);
                user.setFirstName(spec.firstName);
                user.setLastName(spec.lastName);
                user.setRole(spec.role);
                user.setActive(true);
                user.setPassword(passwordEncoder.encode(spec.rawPassword));

                userRepository.save(user);
                log.info("Created development user: {} (role={})", email, spec.role);
                created++;
            }
        }

        log.info("Development user seeding complete. Created: {}, Skipped: {}", created, skipped);
    }

    /**
     * Validates that all required seed passwords are present.
     * Throws {@link IllegalStateException} naming only the missing variable — never its value.
     */
    void validatePasswords() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Development user seeding is enabled but a required configuration value is missing: " +
                    "DEV_ADMIN_PASSWORD");
        }
        if (agentPassword == null || agentPassword.isBlank()) {
            throw new IllegalStateException(
                    "Development user seeding is enabled but a required configuration value is missing: " +
                    "DEV_AGENT_PASSWORD");
        }
        if (userPassword == null || userPassword.isBlank()) {
            throw new IllegalStateException(
                    "Development user seeding is enabled but a required configuration value is missing: " +
                    "DEV_USER_PASSWORD");
        }
    }

    private Map<String, DevUserSpec> buildSpecs() {
        Map<String, DevUserSpec> specs = new LinkedHashMap<>();
        specs.put(adminEmail, new DevUserSpec(adminFirstName, adminLastName, UserRole.ADMIN, adminPassword));
        specs.put(agentEmail, new DevUserSpec(agentFirstName, agentLastName, UserRole.SUPPORT_AGENT, agentPassword));
        specs.put(userEmail, new DevUserSpec(userFirstName, userLastName, UserRole.USER, userPassword));
        return specs;
    }

    /**
     * Internal value object holding the spec for a single dev user to seed.
     * {@code rawPassword} is only ever passed to the {@link PasswordEncoder}; it is never logged.
     */
    static final class DevUserSpec {
        final String firstName;
        final String lastName;
        final UserRole role;
        final String rawPassword;

        DevUserSpec(String firstName, String lastName, UserRole role, String rawPassword) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
            this.rawPassword = rawPassword;
        }
    }
}
