package com.bookstore.context;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(CurrentUser user) { HOLDER.set(user); }

    public static CurrentUser get() { return HOLDER.get(); }

    public static void clear() { HOLDER.remove(); }

    public static Long requireUserId() {
        CurrentUser u = HOLDER.get();
        if (u == null) {
            throw new IllegalStateException("UserContext is empty");
        }
        return u.getUserId();
    }
}
