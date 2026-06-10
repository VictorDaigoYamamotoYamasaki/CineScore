package com.cinescore.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(2)
public class RateLimitUsuarioFilter extends OncePerRequestFilter {

    private static final int          LIMITE_ROTA_CARA = 20;
    private static final List<String> ROTAS_CARAS      = List.of(
            "/api/recommendations",
            "/api/movies/search",
            "/api/movies/trending",
            "/api/movies/actors"
    );

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        boolean rotaCara = ROTAS_CARAS.stream()
                .anyMatch(rota -> req.getRequestURI().startsWith(rota));

        if (!rotaCara) {
            chain.doFilter(req, res);
            return;
        }

        String chave = resolverChave(req);
        Bucket bucket = cache.get("usr:" + chave, k -> criarBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            log.warn("Rate limit de usuário atingido: chave={} uri={}", chave, req.getRequestURI());
            escreverRespostaLimitada(res);
        }
    }

    private String resolverChave(HttpServletRequest req) {
        if (req.getUserPrincipal() != null) {
            return req.getUserPrincipal().getName();
        }
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private Bucket criarBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(LIMITE_ROTA_CARA,
                        Refill.greedy(LIMITE_ROTA_CARA, Duration.ofMinutes(1))))
                .build();
    }

    private void escreverRespostaLimitada(HttpServletResponse res) throws IOException {
        res.setStatus(429);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Você está fazendo muitas requisições. Aguarde um momento.\"}");
    }
}
