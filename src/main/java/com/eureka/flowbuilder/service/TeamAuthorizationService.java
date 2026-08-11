package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.entity.App;
import com.eureka.flowbuilder.model.entity.User;

public interface TeamAuthorizationService {
    boolean canView(Long teamId, User user);

    boolean canEdit(Long teamId, User user);

    boolean canView(App app, User user);

    boolean canEdit(App app, User user);

    void requireView(Long teamId, User user);

    void requireEdit(Long teamId, User user);

    void requireView(App app, User user);

    void requireEdit(App app, User user);
}
