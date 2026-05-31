package com.bookstore.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey key;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private static final List<String> WHITE_LIST = List.of(
        "/api/auth/**",
        "/api/user/register",
        "/api/book/**",
        "/api/category/**",
        "/api/review/**",
        "/api/search-history/**",
        "/api/admin/auth/**",
        "/api/ai/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/swagger-ui/**",
        "/doc.html",
        "/favicon.ico"
    );

    public JwtAuthGatewayFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String h = request.getHeaders().getFirst(HEADER);
        if (h == null || !h.startsWith(PREFIX)) {
            log.debug("missing or invalid auth header for path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = h.substring(PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                throw new JwtException("token type mismatch");
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("invalid jwt for path={}: {}", path, ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
