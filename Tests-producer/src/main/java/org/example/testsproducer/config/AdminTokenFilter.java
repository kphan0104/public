package org.example.testsproducer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class AdminTokenFilter extends OncePerRequestFilter {

    private static final String UNAUTHORIZED_RESPONSE = """
            {"type":"about:blank","title":"Non autorisé","status":401,
            "detail":"Le header X-Admin-Token est absent ou invalide"}
            """;

    private final AdminProperties properties;

    public AdminTokenFilter(AdminProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String candidate = request.getHeader(AdminProperties.TOKEN_HEADER);
        if (properties.matches(candidate)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(UNAUTHORIZED_RESPONSE);
    }
}
