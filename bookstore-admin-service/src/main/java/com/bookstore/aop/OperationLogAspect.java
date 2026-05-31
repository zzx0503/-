package com.bookstore.aop;

import com.bookstore.anno.AdminRequired;
import com.bookstore.context.CurrentUser;
import com.bookstore.context.UserContext;
import com.bookstore.domain.po.OperationLog;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Pointcut("execution(* com.bookstore.controller.admin..*.*(..))")
    public void adminControllers() {
    }

    @Pointcut("@annotation(com.bookstore.anno.OperationLog)")
    public void explicitTagged() {
    }

    @Around("adminControllers() || explicitTagged()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                int duration = (int) (System.currentTimeMillis() - start);
                record(pjp, duration, error);
            } catch (Exception e) {
                log.warn("write operation log failed", e);
            }
        }
    }

    private void record(ProceedingJoinPoint pjp, int durationMs, Throwable error) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest req = attrs.getRequest();
        String method = req.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return;
        }
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method targetMethod = signature.getMethod();
        com.bookstore.anno.OperationLog ann = targetMethod.getAnnotation(com.bookstore.anno.OperationLog.class);
        boolean isAdminController = signature.getDeclaringType().getName().startsWith("com.bookstore.controller.admin")
            || signature.getDeclaringType().isAnnotationPresent(AdminRequired.class)
            || targetMethod.isAnnotationPresent(AdminRequired.class);
        if (ann == null && !isAdminController) {
            return;
        }

        OperationLog logEntry = new OperationLog();
        CurrentUser cu = UserContext.get();
        if (cu != null) {
            logEntry.setAdminUserId(cu.getUserId());
            logEntry.setAdminUsername(cu.getUsername());
        }
        logEntry.setActionType(resolveActionType(ann, method));
        logEntry.setResourceType(resolveResourceType(ann, signature.getDeclaringType().getSimpleName()));
        logEntry.setSummary(ann != null && !ann.summary().isEmpty()
            ? ann.summary()
            : signature.getDeclaringType().getSimpleName() + "#" + signature.getName());
        logEntry.setRequestPath(req.getRequestURI());
        logEntry.setRequestMethod(method);
        logEntry.setIpAddress(extractIp(req));
        logEntry.setDurationMs(durationMs);
        logEntry.setTargetId(extractTargetId(pjp));
        if (error == null) {
            logEntry.setSuccess(1);
        } else {
            logEntry.setSuccess(0);
            String msg = error instanceof BusinessException be
                ? be.getMessage()
                : error.getClass().getSimpleName() + ":" + error.getMessage();
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            logEntry.setErrorMsg(msg);
        }
        operationLogService.recordAsync(logEntry);
    }

    private String resolveActionType(com.bookstore.anno.OperationLog ann, String method) {
        if (ann != null && !ann.action().isEmpty()) {
            return ann.action();
        }
        return switch (method.toUpperCase()) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "ACTION";
        };
    }

    private String resolveResourceType(com.bookstore.anno.OperationLog ann, String className) {
        if (ann != null && !ann.resource().isEmpty()) {
            return ann.resource();
        }
        String name = className;
        if (name.endsWith("AdminController")) {
            name = name.substring(0, name.length() - "AdminController".length());
        } else if (name.endsWith("Controller")) {
            name = name.substring(0, name.length() - "Controller".length());
        }
        return camelToUpperSnake(name);
    }

    private String camelToUpperSnake(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private String extractTargetId(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return null;
        Object first = args[0];
        if (first instanceof Long || first instanceof Integer || first instanceof String) {
            return String.valueOf(first);
        }
        return null;
    }

    private String extractIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        return req.getRemoteAddr();
    }
}
