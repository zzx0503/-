package com.bookstore.service;

import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.api.user.dto.AddressDTO;

import java.util.List;

public interface AddressService {

    List<AddressDTO> list(Long userId);

    AddressDTO create(Long userId, AddressFormDTO dto);

    AddressDTO update(Long userId, Long id, AddressFormDTO dto);

    void delete(Long userId, Long id);

    void setDefault(Long userId, Long id);
}
