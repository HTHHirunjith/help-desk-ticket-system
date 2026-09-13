package com.hansana.helpdesk.auth.security;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filterWithRepo;
    private JwtAuthenticationFilter filterWithoutRepo;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filterWithRepo = new JwtAuthenticationFilter(jwtService, userRepository);
        filterWithoutRepo = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withValidTokenAndActiveUser_authenticatesSuccessfully() throws ServletException, IOException {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        String email = "alice@example.com";

        User dbUser = new User();
        dbUser.setId(userId);
        dbUser.setEmail(email);
        dbUser.setRole(UserRole.USER);
        dbUser.setActive(true);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(UserRole.USER);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser));

        filterWithRepo.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getPrincipal() instanceof UserPrincipal);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(userId, principal.getId());
        assertEquals(email, principal.getEmail());
        assertEquals(UserRole.USER, principal.getRole());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withValidTokenAndInactiveUser_clearsContextAndDoesNotAuthenticate() throws ServletException, IOException {
        // SEC-01: Previously issued valid JWT for deactivated user must NOT authenticate
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        String email = "deactivated@example.com";

        User dbUser = new User();
        dbUser.setId(userId);
        dbUser.setEmail(email);
        dbUser.setRole(UserRole.USER);
        dbUser.setActive(false); // Inactive in DB

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(UserRole.USER);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser));

        filterWithRepo.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication must be null when user is deactivated in DB");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withValidTokenAndDeletedUser_clearsContextAndDoesNotAuthenticate() throws ServletException, IOException {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        String email = "deleted@example.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(UserRole.USER);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        filterWithRepo.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication must be null when user is not found in DB");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withoutUserRepository_authenticatesDirectlyFromTokenClaims() throws ServletException, IOException {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        String email = "slice@example.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(UserRole.SUPPORT_AGENT);
        when(jwtService.extractUserId(token)).thenReturn(userId);

        filterWithoutRepo.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(email, principal.getEmail());
        assertEquals(UserRole.SUPPORT_AGENT, principal.getRole());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withMissingAuthorizationHeader_doesNotAuthenticate() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filterWithRepo.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_withNonBearerScheme_doesNotAuthenticate() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filterWithRepo.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_withBearerPrefixOnly_doesNotAuthenticate() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        filterWithRepo.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withWhitespaceBearerToken_doesNotAuthenticate() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        filterWithRepo.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withInvalidOrExpiredToken_doesNotAuthenticate() throws ServletException, IOException {
        String token = "invalid.or.expired.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(false);

        filterWithRepo.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_whenTokenExtractionThrowsException_clearsContextSafely() throws ServletException, IOException {
        String token = "corrupt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Unexpected parsing error"));

        filterWithRepo.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withUpdatedRoleInDatabase_usesLatestDatabaseRole() throws ServletException, IOException {
        String token = "token.with.old.role";
        UUID userId = UUID.randomUUID();
        String email = "promoted@example.com";

        User dbUser = new User();
        dbUser.setId(userId);
        dbUser.setEmail(email);
        dbUser.setRole(UserRole.ADMIN); // Promoted in DB
        dbUser.setActive(true);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(UserRole.USER); // Old role in token
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser));

        filterWithRepo.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(UserRole.ADMIN, principal.getRole());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
