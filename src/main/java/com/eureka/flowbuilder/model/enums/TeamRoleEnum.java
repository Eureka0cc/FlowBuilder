package com.eureka.flowbuilder.model.enums;

import lombok.Getter;

@Getter
public enum TeamRoleEnum {
    OWNER("OWNER"),
    EDITOR("EDITOR"),
    VIEWER("VIEWER");

    private final String value;

    TeamRoleEnum(String value) {
        this.value = value;
    }

    public static TeamRoleEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (TeamRoleEnum role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return null;
    }

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }
}
