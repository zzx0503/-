package com.bookstore.interceptor;

import com.bookstore.anno.AdminRequired;
import com.bookstore.context.CurrentUser;
import com.bookstore.context.UserContext;
import com.bookstore.exception.AuthException;
import com.bookstore.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        boolean needsAdmin = req.getRequestURI().startsWith("/admin-api/")
            || hm.hasMethodAnnotation(AdminRequired.class)
            || hm.getBeanType().isAnnotationPresent(AdminRequired.class);
        if (!needsAdmin) return true;
        CurrentUser u = UserContext.get();
        if (u == null) throw new AuthException(ResultCode.UNAUTHORIZED);
        if (!"ADMIN".equals(u.getRole())) throw new AuthException(ResultCode.FORBIDDEN);
        return true;
    }
}
