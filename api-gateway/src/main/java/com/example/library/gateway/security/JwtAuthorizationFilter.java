package com.example.library.gateway.security;

import com.example.library.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthorizationFilter implements GlobalFilter, Ordered {
    private final JwtService jwtService;

    public JwtAuthorizationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path) || exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Bearer token required");
        }

        try {
            Claims claims = jwtService.parseToken(authorization.substring(7));
            String role = claims.get("role", String.class);
            if (claims.getSubject() == null || claims.getSubject().isBlank()
                    || !("USER".equals(role) || "STUDENT".equals(role) || "FACULTY".equals(role)
                    || "LIBRARIAN".equals(role) || "ADMIN".equals(role))) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token claims");
            }
            if (path.startsWith("/api/auth/users") && !"ADMIN".equals(role)) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Administrator role required");
            }
            if (isWriteRequest(exchange.getRequest().getMethod())
                    && ("/api/books".equals(path) || path.startsWith("/api/books/"))
                    && !("ADMIN".equals(role) || "LIBRARIAN".equals(role))) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Librarian or administrator role required");
            }
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException exception) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path) {
        return "/".equals(path) || "/index.html".equals(path) || "/favicon.ico".equals(path)
                || "/api/auth/register".equals(path) || "/api/auth/login".equals(path)
                || "/actuator/health".equals(path);
    }

    private boolean isWriteRequest(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH
                || method == HttpMethod.DELETE;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
