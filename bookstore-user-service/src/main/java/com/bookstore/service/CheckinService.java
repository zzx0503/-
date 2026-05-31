package com.bookstore.service;

import com.bookstore.domain.vo.checkin.CheckinResultVO;
import com.bookstore.domain.vo.checkin.CheckinStatusVO;
import com.bookstore.domain.vo.checkin.CheckinRecordVO;
import com.bookstore.response.PageResult;

public interface CheckinService {

    CheckinResultVO checkin(Long userId);

    CheckinStatusVO getStatus(Long userId);

    PageResult<CheckinRecordVO> getHistory(Long userId, Integer page, Integer size);
}
