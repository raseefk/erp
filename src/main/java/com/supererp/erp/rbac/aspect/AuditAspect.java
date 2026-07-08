package com.supererp.erp.rbac.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.entity.AuditLog;
import com.supererp.erp.rbac.annotation.AuditAction;
import com.supererp.erp.repository.AuditLogRepository;
import com.supererp.erp.security.jwt.JwtAuthToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Audit aspect — single-tenant mode.
 * Logs actions to audit_log without any RLS session variable calls.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditRepo;
    private final ObjectMapper       objectMapper;

    @AfterReturning(value = "@annotation(auditAction)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            if (auth instanceof JwtAuthToken jwtAuth) {
                userId = jwtAuth.getUserId();
            }

            HttpServletRequest request = null;
            var requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes sra) {
                request = sra.getRequest();
            }

            String entityId = null;
            Map<String, Object> state = new HashMap<>();

            if (result != null) {
                try {
                    var idField = result.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    entityId = String.valueOf(idField.get(result));
                    state = objectMapper.convertValue(result, Map.class);
                } catch (Exception ignored) {}
            }

            AuditLog logEntry = AuditLog.builder()
                .tenantId(AppTenantConfig.APP_TENANT_ID)
                .userId(userId)
                .action(auditAction.value())
                .entityType(auditAction.entityType().isEmpty()
                    ? joinPoint.getTarget().getClass().getSimpleName()
                    : auditAction.entityType())
                .entityId(entityId)
                .newStateJson(state)
                .ipAddress(request != null ? request.getRemoteAddr() : "UNKNOWN")
                .userAgent(request != null ? request.getHeader("User-Agent") : "UNKNOWN")
                .build();

            auditRepo.save(logEntry);
            log.debug("Audit Log saved: {} for entity {}", auditAction.value(), entityId);

        } catch (Exception e) {
            log.warn("Failed to persist audit log for action '{}': {}", auditAction.value(), e.getMessage());
        }
    }
}
