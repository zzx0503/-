package com.bookstore.domain.vo.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookCoverInitVO {

    private Integer uploaded;
    private Integer booksUpdated;
    private List<String> uploadedKeys = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}
