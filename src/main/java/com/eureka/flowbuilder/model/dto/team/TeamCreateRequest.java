package com.eureka.flowbuilder.model.dto.team;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamCreateRequest implements Serializable {
    private String name;
}
