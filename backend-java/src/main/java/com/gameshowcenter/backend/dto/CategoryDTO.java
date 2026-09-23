package com.gameshowcenter.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDTO {
    private String categoryName;
    private List<QuestionDTO> questions;
}