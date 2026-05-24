package com.bookstore.service;

public interface TokenBlacklistService {

    void revoke(String jti, long expiresAtEpochMs);

    boolean isRevoked(String jti);
}
