package com.bookstore.service;

import com.bookstore.domain.vo.admin.BookCoverInitVO;

public interface AdminBookCoverService {

    BookCoverInitVO bulkInit(String localDir);
}
