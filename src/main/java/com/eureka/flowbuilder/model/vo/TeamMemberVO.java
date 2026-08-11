package com.eureka.flowbuilder.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamMemberVO {
    private Long userId;
    private String userName;
    private String userAvatar;
    private String role;
    private boolean canView;
    private boolean canEdit;
    private LocalDateTime joinedTime;
}
