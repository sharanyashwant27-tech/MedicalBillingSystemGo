package com.medicalbilling.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects requests that carry credentials, tokens, or security keys in the query string.
 * Secrets must be sent in request bodies or headers only — never in URLs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SensitiveQueryParamFilter extends OncePerRequestFilter {

    private static final Set<String> BLOCKED_PARAMS = Set.of(
            "password", "passwd", "pwd", "pass",
            "username", "user", "userid", "user_id",
            "token", "access_token", "refresh_token", "id_token", "jwt", "bearer", "bearer_token",
            "secret", "secret_key", "secretkey", "api_key", "apikey", "api_token", "api-token",
            "signing_key", "signingkey", "client_secret", "clientsecret", "private_key", "privatekey",
            "credentials", "authorization", "auth", "key", "csrf", "_csrf", "session", "jsessionid",
            "x-api-key", "x_auth_token"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (hasBlockedQueryParam(request.getQueryString())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Security keys and credentials must not be sent in the URL. Use POST body or headers.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasBlockedQueryParam(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return false;
        }
        for (String pair : queryString.split("&")) {
            int equalsIndex = pair.indexOf('=');
            String name = equalsIndex >= 0 ? pair.substring(0, equalsIndex) : pair;
            if (BLOCKED_PARAMS.contains(name.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
