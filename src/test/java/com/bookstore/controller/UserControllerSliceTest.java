package com.bookstore.controller;

import com.bookstore.config.AvatarProperties;
import com.bookstore.context.CurrentUser;
import com.bookstore.context.UserContext;
import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.domain.dto.user.UpdateAvatarDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.service.OSSService;
import com.bookstore.service.UserService;
import com.bookstore.utils.OssUrlBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerSliceTest {

    UserService userService;
    OSSService ossService;
    OssUrlBuilder ossUrlBuilder;
    AvatarProperties avatarProperties;
    MockMvc mvc;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        ossService = mock(OSSService.class);
        ossUrlBuilder = mock(OssUrlBuilder.class);
        avatarProperties = mock(AvatarProperties.class);
        UserController controller = new UserController(userService, ossService, ossUrlBuilder, avatarProperties);
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        UserContext.set(new CurrentUser(1L, "alice", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UserProfileVO sample() {
        UserProfileVO v = new UserProfileVO();
        v.setId(1L);
        v.setUsername("alice");
        v.setNickname("Alice");
        v.setPhone("138****5678");
        v.setRole("USER");
        return v;
    }

    @Test
    void me_returns_profile() throws Exception {
        when(userService.getProfile(1L)).thenReturn(sample());

        mvc.perform(get("/api/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("alice"))
            .andExpect(jsonPath("$.data.phone").value("138****5678"));
    }

    @Test
    void updateMe_validation_fails_when_gender_out_of_range() throws Exception {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setGender(5);

        mvc.perform(put("/api/user/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateAvatar_rejects_bad_prefix() throws Exception {
        UpdateAvatarDTO dto = new UpdateAvatarDTO();
        dto.setAvatarKey("../etc/passwd");

        mvc.perform(put("/api/user/me/avatar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateAvatar_accepts_valid_key() throws Exception {
        when(userService.updateAvatar(eq(1L), eq("avatars/1.jpg"))).thenReturn(sample());

        UpdateAvatarDTO dto = new UpdateAvatarDTO();
        dto.setAvatarKey("avatars/1.jpg");

        mvc.perform(put("/api/user/me/avatar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("alice"));
    }
}
