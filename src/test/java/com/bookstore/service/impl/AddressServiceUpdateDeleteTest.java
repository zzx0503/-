package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceUpdateDeleteTest {

    @Mock AddressMapper addressMapper;
    @InjectMocks AddressServiceImpl addressService;

    private Address own(Long userId, Long id, int isDefault) {
        Address a = new Address();
        a.setId(id);
        a.setUserId(userId);
        a.setIsDefault(isDefault);
        a.setReceiver("张三");
        a.setPhone("13800000001");
        a.setProvince("浙江");
        a.setCity("杭州");
        a.setDistrict("西湖区");
        a.setDetailAddress("...");
        return a;
    }

    @Test
    void update_changes_fields() {
        when(addressMapper.selectById(10L)).thenReturn(own(1L, 10L, 0));

        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("李四");
        dto.setPhone("13900000000");
        dto.setProvince("浙江");
        dto.setCity("杭州");
        dto.setDistrict("拱墅区");
        dto.setDetailAddress("莫干山路 100 号");

        AddressVO vo = addressService.update(1L, 10L, dto);
        assertThat(vo.getReceiver()).isEqualTo("李四");
        verify(addressMapper).updateById(any(Address.class));
    }

    @Test
    void update_rejects_other_user_address() {
        when(addressMapper.selectById(10L)).thenReturn(own(2L, 10L, 0));

        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("X");
        dto.setPhone("13800000001");
        dto.setProvince("X");
        dto.setCity("X");
        dto.setDistrict("X");
        dto.setDetailAddress("X");

        assertThatThrownBy(() -> addressService.update(1L, 10L, dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.FORBIDDEN);
    }

    @Test
    void delete_default_promotes_next() {
        when(addressMapper.selectById(10L)).thenReturn(own(1L, 10L, 1));
        Address other = own(1L, 12L, 0);
        when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(other));

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteById(10L);
        verify(addressMapper).updateById(argThat(a ->
            a.getId().equals(12L) && a.getIsDefault() != null && a.getIsDefault() == 1));
    }

    @Test
    void delete_non_default_does_not_touch_others() {
        when(addressMapper.selectById(11L)).thenReturn(own(1L, 11L, 0));
        addressService.delete(1L, 11L);
        verify(addressMapper).deleteById(11L);
        verify(addressMapper, never()).updateById(any(Address.class));
    }
}
