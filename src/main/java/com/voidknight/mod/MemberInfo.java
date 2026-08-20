package com.voidknight.mod;

public class MemberInfo {

    // API se role directly read hoga
    public String role;

    // Compatibility: agar API me mode aaye
    public String mode;

    // Compatibility: purana API field
    public String type;

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
