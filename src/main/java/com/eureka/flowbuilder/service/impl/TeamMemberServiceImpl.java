package com.eureka.flowbuilder.service.impl;

import com.eureka.flowbuilder.mapper.TeamMemberMapper;
import com.eureka.flowbuilder.model.entity.TeamMember;
import com.eureka.flowbuilder.service.TeamMemberService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TeamMemberServiceImpl extends ServiceImpl<TeamMemberMapper, TeamMember> implements TeamMemberService {
    @Override
    public TeamMember getMembership(Long teamId, Long userId) {
        if (teamId == null || userId == null) {
            return null;
        }
        return getOne(QueryWrapper.create()
                .eq("teamId", teamId)
                .eq("userId", userId));
    }
}
