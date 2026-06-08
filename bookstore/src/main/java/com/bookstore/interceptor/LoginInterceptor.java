package com.bookstore.interceptor;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.exception.AuthException;
import com.bookstore.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        Method method = hm.getMethod();
        boolean methodAnnotated = method.isAnnotationPresent(LoginRequired.class);
        boolean classAnnotated = method.getDeclaringClass().isAnnotationPresent(LoginRequired.class);
        if (!methodAnnotated && !classAnnotated) return true;
        if (UserContext.get() == null) {
            throw new AuthException(ResultCode.UNAUTHORIZED);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        UserContext.clear();
    }
}
