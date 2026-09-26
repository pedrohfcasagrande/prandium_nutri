package com.prandiumnutri.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Configuration
public class TraceIdFilter {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<OncePerRequestFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                String traceId = req.getHeader(HEADER);
                if (!StringUtils.hasText(traceId)) {
                    traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                }
                MDC.put(MDC_KEY, traceId);
                res.setHeader(HEADER, traceId);
                try {
                    chain.doFilter(req, res);
                } finally {
                    MDC.remove(MDC_KEY);
                }
            }
        });
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
