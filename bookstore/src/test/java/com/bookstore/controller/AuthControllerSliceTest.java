package com.bookstore.controller;

import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.domain.vo.auth.UserBriefVO;
import com.bookstore.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerSliceTest {

    AuthService authService;
    MockMvc mvc;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    private TokenVO sampleTokenVO() {
        UserBriefVO brief = new UserBriefVO();
        brief.setId(1L);
        brief.setUsername("alice");
        brief.setRole("USER");
        return new TokenVO("acc", "ref", 7200L, brief);
    }

    @Test
    void register_returns_200_with_token() throws Exception {
        when(authService.register(any())).thenReturn(sampleTokenVO());

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("YOUR_PASSWORD");
        dto.setPhone("13800000001");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").value("acc"))
            .andExpect(jsonPath("$.data.refreshToken").value("ref"))
            .andExpect(jsonPath("$.data.user.username").value("alice"));
    }

    @Test
    void register_validation_fails_when_username_too_short() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ab");
        dto.setPassword("YOUR_PASSWORD");
        dto.setPhone("13800000001");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void login_returns_200_with_token() throws Exception {
        when(authService.login(any())).thenReturn(sampleTokenVO());

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("YOUR_PASSWORD");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }

    @Test
    void logout_extracts_bearer_and_returns_200() throws Exception {
        mvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer some-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(authService).logout("some-token");
    }
}
