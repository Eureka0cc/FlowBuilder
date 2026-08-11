package com.eureka.flowbuilder.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamVO {
    private Long id;
    private String name;
    private Long ownerId;
    private Integer isPersonal;
    private String currentUserRole;
    private boolean canEdit;
    private LocalDateTime createTime;
}
