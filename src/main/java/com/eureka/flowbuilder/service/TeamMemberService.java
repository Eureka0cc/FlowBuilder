package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.entity.TeamMember;
import com.mybatisflex.core.service.IService;

public interface TeamMemberService extends IService<TeamMember> {
    TeamMember getMembership(Long teamId, Long userId);
}
