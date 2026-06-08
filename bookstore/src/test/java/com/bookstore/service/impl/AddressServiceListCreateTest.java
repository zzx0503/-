package com.bookstore.service.impl;

import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceListCreateTest {

    @Mock AddressMapper addressMapper;
    @InjectMocks AddressServiceImpl addressService;

    private AddressFormDTO sampleForm() {
        AddressFormDTO f = new AddressFormDTO();
        f.setReceiver("张三");
        f.setPhone("13800000001");
        f.setProvince("浙江");
        f.setCity("杭州");
        f.setDistrict("西湖区");
        f.setDetailAddress("文一西路 100 号");
        return f;
    }

    @Test
    void create_first_address_is_default() {
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.<Address>getArgument(0).setId(101L);
            return 1;
        });

        AddressFormDTO f = sampleForm();
        f.setSetDefault(false);

        AddressVO vo = addressService.create(1L, f);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
        assertThat(vo.getIsDefault()).isTrue();
        verify(addressMapper).unsetOtherDefaults(eq(1L), eq(101L));
    }

    @Test
    void create_second_non_default_keeps_existing_default() {
        when(addressMapper.selectCount(any())).thenReturn(1L);
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.<Address>getArgument(0).setId(202L);
            return 1;
        });

        AddressFormDTO f = sampleForm();
        f.setSetDefault(false);

        AddressVO vo = addressService.create(1L, f);

        assertThat(vo.getIsDefault()).isFalse();
        verify(addressMapper, never()).unsetOtherDefaults(anyLong(), anyLong());
    }

    @Test
    void list_orders_default_first() {
        Address a1 = new Address();
        a1.setId(1L); a1.setUserId(1L); a1.setIsDefault(0);
        a1.setReceiver("张三"); a1.setPhone("13800000001");
        a1.setProvince("浙江"); a1.setCity("杭州"); a1.setDistrict("西湖区");
        a1.setDetailAddress("...");
        Address a2 = new Address();
        a2.setId(2L); a2.setUserId(1L); a2.setIsDefault(1);
        a2.setReceiver("李四"); a2.setPhone("13800000002");
        a2.setProvince("浙江"); a2.setCity("杭州"); a2.setDistrict("拱墅区");
        a2.setDetailAddress("...");
        when(addressMapper.selectList(any())).thenReturn(List.of(a2, a1));

        List<AddressVO> list = addressService.list(1L);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getIsDefault()).isTrue();
    }
}
