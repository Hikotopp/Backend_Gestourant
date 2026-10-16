package com.gestourant.audit;

import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrudAuditFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final CrudAuditFilter filter = new CrudAuditFilter(users, audit);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsSuccessfulAuthenticatedApiMutations() throws Exception {
        User user = mock(User.class);
        when(users.findByEmailIgnoreCase("maria@example.com")).thenReturn(Optional.of(user));
        authenticate("maria@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        request.setRemoteAddr("192.0.2.7");

        filter.doFilter(request, new MockHttpServletResponse(), emptyChain());

        verify(audit).log(user, "CREATE", "/api/products", "Operación completada", "192.0.2.7");
    }

    @Test
    void doesNotRecordFailedRequestsOrReadOnlyMethods() throws Exception {
        authenticate("maria@example.com");
        MockHttpServletRequest failedRequest = new MockHttpServletRequest("DELETE", "/api/products/3");
        MockHttpServletResponse failedResponse = new MockHttpServletResponse();
        failedResponse.setStatus(400);

        filter.doFilter(failedRequest, failedResponse, emptyChain());
        filter.doFilter(
            new MockHttpServletRequest("GET", "/api/products"),
            new MockHttpServletResponse(),
            emptyChain()
        );

        verify(audit, never()).log(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
        verify(users, never()).findByEmailIgnoreCase("maria@example.com");
    }

    @Test
    void doesNotRecordUnauthenticatedMutations() throws Exception {
        filter.doFilter(
            new MockHttpServletRequest("PUT", "/api/products/3"),
            new MockHttpServletResponse(),
            emptyChain()
        );

        verify(audit, never()).log(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(email, "credentials", List.of())
        );
    }

    private static FilterChain emptyChain() {
        return (request, response) -> { };
    }
}
