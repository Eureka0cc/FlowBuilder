package com.eureka.flowbuilder.model.dto.team;

import lombok.Data;

@Data
public class TeamAssetUpdateRequest {
    private Long id;
    private String name;
    private String tags;
}
