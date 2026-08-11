package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.entity.App;
import com.eureka.flowbuilder.model.entity.TeamMember;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.enums.TeamRoleEnum;
import com.eureka.flowbuilder.service.impl.TeamAuthorizationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamAuthorizationServiceImplTest {
    private final TeamMemberService memberService = mock(TeamMemberService.class);
    private final TeamAuthorizationServiceImpl authorizationService = new TeamAuthorizationServiceImpl();

    TeamAuthorizationServiceImplTest() {
        ReflectionTestUtils.setField(authorizationService, "teamMemberService", memberService);
    }

    @Test
    void appliesRoleCapabilities() {
        User user = user(1L, "user");
        when(memberService.getMembership(10L, 1L))
                .thenReturn(member(TeamRoleEnum.VIEWER));
        when(memberService.getMembership(11L, 1L))
                .thenReturn(member(TeamRoleEnum.EDITOR));

        assertTrue(authorizationService.canView(10L, user));
        assertFalse(authorizationService.canEdit(10L, user));
        assertTrue(authorizationService.canEdit(11L, user));
    }

    @Test
    void legacyAppsRemainCreatorOrAdminOnly() {
        App app = new App();
        app.setUserId(1L);
        app.setTeamId(null);

        assertTrue(authorizationService.canEdit(app, user(1L, "user")));
        assertFalse(authorizationService.canView(app, user(2L, "user")));
        assertTrue(authorizationService.canView(app, user(2L, "admin")));
    }

    private TeamMember member(TeamRoleEnum role) {
        TeamMember member = new TeamMember();
        member.setRole(role.getValue());
        return member;
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setUserRole(role);
        return user;
    }
}
