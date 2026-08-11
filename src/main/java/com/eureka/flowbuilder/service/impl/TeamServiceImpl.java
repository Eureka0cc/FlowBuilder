package com.eureka.flowbuilder.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.mapper.TeamMapper;
import com.eureka.flowbuilder.model.entity.Team;
import com.eureka.flowbuilder.model.entity.TeamMember;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.enums.TeamRoleEnum;
import com.eureka.flowbuilder.model.vo.TeamMemberVO;
import com.eureka.flowbuilder.model.vo.TeamVO;
import com.eureka.flowbuilder.service.TeamAuthorizationService;
import com.eureka.flowbuilder.service.TeamMemberService;
import com.eureka.flowbuilder.service.TeamService;
import com.eureka.flowbuilder.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {
    @Resource
    private TeamMemberService teamMemberService;
    @Resource
    private TeamAuthorizationService authorizationService;
    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTeam(String name, User creator, boolean personal) {
        ThrowUtils.throwIf(creator == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(name) || name.length() > 80,
                ErrorCode.PARAMS_ERROR, "团队名称长度应为 1-80");
        Team team = new Team();
        team.setName(name.trim());
        team.setOwnerId(creator.getId());
        team.setIsPersonal(personal ? 1 : 0);
        ThrowUtils.throwIf(!save(team), ErrorCode.OPERATION_ERROR, "创建团队失败");
        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserId(creator.getId());
        owner.setRole(TeamRoleEnum.OWNER.getValue());
        ThrowUtils.throwIf(!teamMemberService.save(owner), ErrorCode.OPERATION_ERROR, "创建团队成员失败");
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreatePersonalTeam(User user) {
        Team existing = getOne(QueryWrapper.create()
                .eq("ownerId", user.getId())
                .eq("isPersonal", 1));
        if (existing != null) {
            return existing.getId();
        }
        String displayName = StrUtil.blankToDefault(user.getUserName(), user.getUserAccount());
        return createTeam(displayName + " 的个人团队", user, true);
    }

    @Override
    public List<TeamVO> listMyTeams(User user) {
        List<TeamMember> memberships = teamMemberService.list(QueryWrapper.create().eq("userId", user.getId()));
        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, TeamMember> membershipMap = memberships.stream()
                .collect(Collectors.toMap(TeamMember::getTeamId, Function.identity(), (a, b) -> a));
        return listByIds(membershipMap.keySet()).stream().map(team -> {
            TeamVO vo = new TeamVO();
            BeanUtil.copyProperties(team, vo);
            TeamRoleEnum role = TeamRoleEnum.fromValue(membershipMap.get(team.getId()).getRole());
            vo.setCurrentUserRole(role == null ? null : role.getValue());
            vo.setCanEdit(role != null && role.canEdit());
            return vo;
        }).toList();
    }

    @Override
    public List<TeamMemberVO> listMembers(Long teamId, User user) {
        ThrowUtils.throwIf(getById(teamId) == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");
        authorizationService.requireView(teamId, user);
        List<TeamMember> members = teamMemberService.list(QueryWrapper.create().eq("teamId", teamId));
        Map<Long, User> users = userService.listByIds(
                        members.stream().map(TeamMember::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream().map(member -> {
            TeamMemberVO vo = new TeamMemberVO();
            User memberUser = users.get(member.getUserId());
            vo.setUserId(member.getUserId());
            vo.setRole(member.getRole());
            vo.setJoinedTime(member.getCreateTime());
            vo.setCanView(true);
            TeamRoleEnum role = TeamRoleEnum.fromValue(member.getRole());
            vo.setCanEdit(role != null && role.canEdit());
            if (memberUser != null) {
                vo.setUserName(memberUser.getUserName());
                vo.setUserAvatar(memberUser.getUserAvatar());
            }
            return vo;
        }).toList();
    }
}
