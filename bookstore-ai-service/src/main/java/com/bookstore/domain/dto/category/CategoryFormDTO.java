package com.bookstore.domain.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryFormDTO {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    private Long parentId;

    @Size(max = 255)
    private String iconKey;

    @NotNull
    private Integer sort;

    @NotNull
    private Integer status;
}
