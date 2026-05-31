package com.bookstore.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtil {

    private IpUtil() {}

    public static String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma > -1 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        return req.getRemoteAddr();
    }
}
