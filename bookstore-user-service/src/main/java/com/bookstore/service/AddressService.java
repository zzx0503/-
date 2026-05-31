package com.bookstore.service;

import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.vo.address.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> list(Long userId);

    AddressVO create(Long userId, AddressFormDTO dto);

    AddressVO update(Long userId, Long id, AddressFormDTO dto);

    void delete(Long userId, Long id);

    void setDefault(Long userId, Long id);
}
