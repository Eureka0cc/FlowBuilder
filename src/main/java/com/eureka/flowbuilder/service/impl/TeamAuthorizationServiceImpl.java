package com.eureka.flowbuilder.service.impl;

import com.eureka.flowbuilder.constant.UserConstant;
import com.eureka.flowbuilder.exception.BusinessException;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.model.entity.App;
import com.eureka.flowbuilder.model.entity.TeamMember;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.enums.TeamRoleEnum;
import com.eureka.flowbuilder.service.TeamAuthorizationService;
import com.eureka.flowbuilder.service.TeamMemberService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TeamAuthorizationServiceImpl implements TeamAuthorizationService {
    @Resource
    private TeamMemberService teamMemberService;

    @Override
    public boolean canView(Long teamId, User user) {
        if (teamId == null || user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return teamMemberService.getMembership(teamId, user.getId()) != null;
    }

    @Override
    public boolean canEdit(Long teamId, User user) {
        if (teamId == null || user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        TeamMember member = teamMemberService.getMembership(teamId, user.getId());
        TeamRoleEnum role = member == null ? null : TeamRoleEnum.fromValue(member.getRole());
        return role != null && role.canEdit();
    }

    @Override
    public boolean canView(App app, User user) {
        if (app == null || user == null) {
            return false;
        }
        if (app.getTeamId() == null) {
            return isAdmin(user) || user.getId().equals(app.getUserId());
        }
        return canView(app.getTeamId(), user);
    }

    @Override
    public boolean canEdit(App app, User user) {
        if (app == null || user == null) {
            return false;
        }
        if (app.getTeamId() == null) {
            return isAdmin(user) || user.getId().equals(app.getUserId());
        }
        return canEdit(app.getTeamId(), user);
    }

    @Override
    public void requireView(Long teamId, User user) {
        require(canView(teamId, user), "无团队查看权限");
    }

    @Override
    public void requireEdit(Long teamId, User user) {
        require(canEdit(teamId, user), "无团队编辑权限");
    }

    @Override
    public void requireView(App app, User user) {
        require(canView(app, user), "无应用查看权限");
    }

    @Override
    public void requireEdit(App app, User user) {
        require(canEdit(app, user), "无应用编辑权限");
    }

    private boolean isAdmin(User user) {
        return UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }

    private void require(boolean allowed, String message) {
        if (!allowed) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, message);
        }
    }
}
