package com.bookstore.domain.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameSessionDTO {

    @NotBlank
    @Size(max = 60, message = "标题过长")
    private String title;
}
