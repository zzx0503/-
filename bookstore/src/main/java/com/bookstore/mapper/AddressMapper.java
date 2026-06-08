package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.Address;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AddressMapper extends BaseMapper<Address> {

    @Update("UPDATE address SET is_default = 0 " +
            "WHERE user_id = #{userId} AND id <> #{keepId} AND deleted = 0")
    int unsetOtherDefaults(@Param("userId") Long userId,
                           @Param("keepId") Long keepId);
}
