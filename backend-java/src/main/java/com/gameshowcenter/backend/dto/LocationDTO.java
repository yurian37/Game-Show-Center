package com.gameshowcenter.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class LocationDTO {
    private String locationName;
    private List<String> images;
}