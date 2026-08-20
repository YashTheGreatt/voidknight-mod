package com.voidknight.mod;

public class MemberInfo {

    public String role;
    public String mode;
    public String type;

    public MemberInfo() {
    }

    public String getRole() {
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }

        if (mode != null && !mode.trim().isEmpty()) {
            return mode.trim();
        }

        if (type != null && !type.trim().isEmpty()) {
            return type.trim();
        }

        return "";
    }
}
