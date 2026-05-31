package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressDTO> list(Long userId) {
        List<Address> rows = addressMapper.selectList(
            new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId)
        );
        return rows.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public AddressDTO create(Long userId, AddressFormDTO dto) {
        Address a = new Address();
        a.setUserId(userId);
        a.setReceiver(dto.getReceiver());
        a.setPhone(dto.getPhone());
        a.setProvince(dto.getProvince());
        a.setCity(dto.getCity());
        a.setDistrict(dto.getDistrict());
        a.setDetailAddress(dto.getDetailAddress());

        boolean wantDefault = Boolean.TRUE.equals(dto.getSetDefault());
        Long count = addressMapper.selectCount(
            new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
        );
        a.setIsDefault((wantDefault || count == 0) ? 1 : 0);

        addressMapper.insert(a);

        if (a.getIsDefault() == 1) {
            addressMapper.unsetOtherDefaults(userId, a.getId());
        }
        return toVO(a);
    }

    @Override
    @Transactional
    public AddressDTO update(Long userId, Long id, AddressFormDTO dto) {
        Address a = requireOwn(userId, id);

        Address patch = new Address();
        patch.setId(id);
        patch.setReceiver(dto.getReceiver());
        patch.setPhone(dto.getPhone());
        patch.setProvince(dto.getProvince());
        patch.setCity(dto.getCity());
        patch.setDistrict(dto.getDistrict());
        patch.setDetailAddress(dto.getDetailAddress());
        addressMapper.updateById(patch);

        if (Boolean.TRUE.equals(dto.getSetDefault()) && (a.getIsDefault() == null || a.getIsDefault() != 1)) {
            setDefault(userId, id);
        }

        a.setReceiver(dto.getReceiver());
        a.setPhone(dto.getPhone());
        a.setProvince(dto.getProvince());
        a.setCity(dto.getCity());
        a.setDistrict(dto.getDistrict());
        a.setDetailAddress(dto.getDetailAddress());
        if (Boolean.TRUE.equals(dto.getSetDefault())) {
            a.setIsDefault(1);
        }
        return toVO(a);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Address a = requireOwn(userId, id);
        addressMapper.deleteById(id);

        if (a.getIsDefault() != null && a.getIsDefault() == 1) {
            List<Address> others = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .ne(Address::getId, id)
                    .orderByDesc(Address::getId)
                    .last("LIMIT 1")
            );
            if (!others.isEmpty()) {
                Address promote = new Address();
                promote.setId(others.get(0).getId());
                promote.setIsDefault(1);
                addressMapper.updateById(promote);
            }
        }
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        requireOwn(userId, id);
        Address patch = new Address();
        patch.setId(id);
        patch.setIsDefault(1);
        addressMapper.updateById(patch);
        addressMapper.unsetOtherDefaults(userId, id);
    }

    Address requireOwn(Long userId, Long id) {
        Address a = addressMapper.selectById(id);
        if (a == null || !a.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return a;
    }

    AddressDTO toVO(Address a) {
        AddressDTO v = new AddressDTO();
        v.setId(a.getId());
        v.setReceiver(a.getReceiver());
        v.setPhone(a.getPhone());
        v.setProvince(a.getProvince());
        v.setCity(a.getCity());
        v.setDistrict(a.getDistrict());
        v.setDetailAddress(a.getDetailAddress());
        v.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        v.setFullAddress(a.getProvince() + a.getCity() + a.getDistrict() + a.getDetailAddress());
        return v;
    }
}
