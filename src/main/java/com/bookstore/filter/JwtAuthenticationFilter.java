package com.bookstore.filter;

import com.bookstore.context.CurrentUser;
import com.bookstore.context.UserContext;
import com.bookstore.utils.JwtUtil;
import com.bookstore.utils.JwtUtil.UserClaims;
import com.bookstore.service.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader(HEADER);
        if (h != null && h.startsWith(PREFIX)) {
            String token = h.substring(PREFIX.length()).trim();
            try {
                UserClaims c = jwtUtil.parseAccess(token);
                if (blacklist.isRevoked(c.jti())) {
                    log.debug("revoked token jti={}", c.jti());
                } else {
                    UserContext.set(new CurrentUser(c.userId(), c.username(), c.role()));
                }
            } catch (JwtException ex) {
                log.debug("invalid jwt: {}", ex.getMessage());
            }
        }
        try {
            chain.doFilter(req, res);
        } finally {
            UserContext.clear();
        }
    }
}
