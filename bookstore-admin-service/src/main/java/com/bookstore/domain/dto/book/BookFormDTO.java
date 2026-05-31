package com.bookstore.domain.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookFormDTO {

    @NotBlank
    @Size(max = 20)
    private String isbn;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String subtitle;

    @NotBlank
    @Size(max = 100)
    private String author;

    @Size(max = 100)
    private String translator;

    @Size(max = 100)
    private String publisher;

    @NotNull
    private Long categoryId;

    @Size(max = 255)
    private String coverKey;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    @NotNull
    @PositiveOrZero
    private BigDecimal originalPrice;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    private String description;

    private LocalDate publishDate;

    @NotNull
    private Integer status;
}
