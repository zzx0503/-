package com.bookstore.controller;

import com.bookstore.context.CurrentUser;
import com.bookstore.context.UserContext;
import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.service.AddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressControllerSliceTest {

    AddressService addressService;
    MockMvc mvc;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        addressService = mock(AddressService.class);
        AddressController controller = new AddressController(addressService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        UserContext.set(new CurrentUser(1L, "alice", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private AddressVO sampleVO() {
        AddressVO v = new AddressVO();
        v.setId(101L);
        v.setReceiver("张三");
        v.setPhone("13800000001");
        v.setProvince("浙江");
        v.setCity("杭州");
        v.setDistrict("西湖区");
        v.setDetailAddress("文一西路 100");
        v.setIsDefault(true);
        v.setFullAddress("浙江杭州西湖区文一西路 100");
        return v;
    }

    @Test
    void list_returns_array() throws Exception {
        when(addressService.list(1L)).thenReturn(List.of(sampleVO()));

        mvc.perform(get("/api/address"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value(101));
    }

    @Test
    void create_validates_phone_format() throws Exception {
        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("张三");
        dto.setPhone("12345");
        dto.setProvince("浙");
        dto.setCity("杭");
        dto.setDistrict("西");
        dto.setDetailAddress("...");

        mvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void create_succeeds_with_valid_form() throws Exception {
        when(addressService.create(eq(1L), any(AddressFormDTO.class))).thenReturn(sampleVO());

        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("张三");
        dto.setPhone("13800000001");
        dto.setProvince("浙江");
        dto.setCity("杭州");
        dto.setDistrict("西湖区");
        dto.setDetailAddress("文一西路 100");
        dto.setSetDefault(true);

        mvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void setDefault_returns_ok() throws Exception {
        mvc.perform(put("/api/address/101/default"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        verify(addressService).setDefault(1L, 101L);
    }

    @Test
    void delete_returns_ok() throws Exception {
        mvc.perform(delete("/api/address/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        verify(addressService).delete(1L, 101L);
    }
}
