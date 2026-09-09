package com.hansana.helpdesk.user.service;

import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
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
}
