package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.entity.Team;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.vo.TeamMemberVO;
import com.eureka.flowbuilder.model.vo.TeamVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface TeamService extends IService<Team> {
    Long createTeam(String name, User creator, boolean personal);

    Long getOrCreatePersonalTeam(User user);

    List<TeamVO> listMyTeams(User user);

    List<TeamMemberVO> listMembers(Long teamId, User user);
}
