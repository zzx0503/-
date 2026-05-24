package com.bookstore.app.it;

import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIT extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper om;

    static String accessToken;
    static String refreshToken;
    static Long addressId;

    private HttpHeaders bearer() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(accessToken);
        return h;
    }

    private HttpHeaders json() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private int code(String body) throws Exception {
        return om.readTree(body).get("code").asInt();
    }

    private <T> T extractData(String body, Class<T> clazz) throws Exception {
        Result<?> r = om.readValue(body, Result.class);
        return om.convertValue(r.getData(), clazz);
    }

    @Test
    @Order(1)
    void register_returns_token_and_user() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("hello123");
        dto.setPhone("13800000001");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/auth/register", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(dto), json()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(resp.getBody())).isEqualTo(ResultCode.SUCCESS.getCode());
        TokenVO vo = extractData(resp.getBody(), TokenVO.class);
        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getRefreshToken()).isNotBlank();
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
        assertThat(vo.getUser().getRole()).isEqualTo("USER");

        accessToken = vo.getAccessToken();
        refreshToken = vo.getRefreshToken();
    }

    @Test
    @Order(2)
    void duplicate_registration_returns_business_error() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("hello123");
        dto.setPhone("13800000099");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/auth/register", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(dto), json()), String.class);

        assertThat(code(resp.getBody())).isEqualTo(ResultCode.USERNAME_TAKEN.getCode());
        assertThat(resp.getBody()).contains("用户名已被注册");
    }

    @Test
    @Order(3)
    void me_returns_masked_phone() throws Exception {
        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/user/me", HttpMethod.GET,
            new HttpEntity<>(bearer()), String.class);

        assertThat(code(resp.getBody())).isEqualTo(ResultCode.SUCCESS.getCode());
        UserProfileVO vo = extractData(resp.getBody(), UserProfileVO.class);
        assertThat(vo.getPhone()).matches("138\\*\\*\\*\\*0001");
        assertThat(vo.getRole()).isEqualTo("USER");
    }

    @Test
    @Order(4)
    void update_profile_changes_nickname() throws Exception {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("小明");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/user/me", HttpMethod.PUT,
            new HttpEntity<>(om.writeValueAsString(dto), bearer()), String.class);

        assertThat(code(resp.getBody())).isEqualTo(ResultCode.SUCCESS.getCode());
        UserProfileVO vo = extractData(resp.getBody(), UserProfileVO.class);
        assertThat(vo.getNickname()).isEqualTo("小明");
    }

    @Test
    @Order(5)
    void create_address_first_is_default() throws Exception {
        AddressFormDTO f = new AddressFormDTO();
        f.setReceiver("张三");
        f.setPhone("13800000001");
        f.setProvince("浙江");
        f.setCity("杭州");
        f.setDistrict("西湖区");
        f.setDetailAddress("文一西路 100 号");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/address", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(f), bearer()), String.class);

        assertThat(code(resp.getBody())).isEqualTo(ResultCode.SUCCESS.getCode());
        AddressVO vo = extractData(resp.getBody(), AddressVO.class);
        assertThat(vo.getIsDefault()).isTrue();
        addressId = vo.getId();
    }

    @Test
    @Order(6)
    void list_address_includes_created() throws Exception {
        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/address", HttpMethod.GET,
            new HttpEntity<>(bearer()), String.class);

        Result<?> r = om.readValue(resp.getBody(), Result.class);
        List<AddressVO> list = om.convertValue(r.getData(), new TypeReference<>() {
        });
        assertThat(list).extracting(AddressVO::getId).contains(addressId);
    }

    @Test
    @Order(7)
    void unauth_request_returns_401() throws Exception {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/user/me", String.class);
        assertThat(code(resp.getBody())).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
    }

    @Test
    @Order(8)
    void logout_then_token_blacklisted() throws Exception {
        restTemplate.exchange("/api/auth/logout", HttpMethod.POST,
            new HttpEntity<>(bearer()), String.class);

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/user/me", HttpMethod.GET,
            new HttpEntity<>(bearer()), String.class);

        assertThat(code(resp.getBody())).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
    }

    @Test
    @Order(9)
    void login_again_then_refresh_returns_new_pair() throws Exception {
        LoginDTO login = new LoginDTO();
        login.setAccount("alice");
        login.setPassword("hello123");
        ResponseEntity<String> r1 = restTemplate.exchange(
            "/api/auth/login", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(login), json()), String.class);
        TokenVO vo1 = extractData(r1.getBody(), TokenVO.class);
        assertThat(vo1.getRefreshToken()).isNotBlank();

        String body = "{\"refreshToken\":\"" + vo1.getRefreshToken() + "\"}";
        ResponseEntity<String> r2 = restTemplate.exchange(
            "/api/auth/refresh", HttpMethod.POST,
            new HttpEntity<>(body, json()), String.class);
        TokenVO vo2 = extractData(r2.getBody(), TokenVO.class);

        assertThat(vo2.getAccessToken()).isNotEqualTo(vo1.getAccessToken());
        assertThat(vo2.getRefreshToken()).isNotEqualTo(vo1.getRefreshToken());

        ResponseEntity<String> r3 = restTemplate.exchange(
            "/api/auth/refresh", HttpMethod.POST,
            new HttpEntity<>(body, json()), String.class);
        assertThat(code(r3.getBody())).isEqualTo(ResultCode.TOKEN_INVALID.getCode());
    }
}
