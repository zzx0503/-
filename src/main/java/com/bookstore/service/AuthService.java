package com.bookstore.service;

import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.vo.auth.TokenVO;

public interface AuthService {

    TokenVO register(RegisterDTO dto);

    TokenVO login(LoginDTO dto);

    TokenVO refresh(RefreshTokenDTO dto);

    void logout(String accessToken);
}
