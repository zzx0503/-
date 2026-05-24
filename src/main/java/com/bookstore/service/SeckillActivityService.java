package com.bookstore.service;

import com.bookstore.domain.dto.seckill.SeckillActivityDTO;
import com.bookstore.domain.po.SeckillActivity;
import com.bookstore.domain.vo.seckill.SeckillActivityVO;
import com.bookstore.response.PageResult;

import java.util.List;

public interface SeckillActivityService {

    Long create(SeckillActivityDTO dto);

    void update(Long id, SeckillActivityDTO dto);

    void delete(Long id);

    void start(Long id);

    void end(Long id);

    SeckillActivityVO detail(Long id, Long userId);

    PageResult<SeckillActivityVO> listAdmin(String status, Integer page, Integer size);

    List<SeckillActivityVO> listRunning(Long userId);

    List<SeckillActivityVO> listUpcoming(Long userId);

    SeckillActivity requireById(Long id);
}
