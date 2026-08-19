package com.voidknight.mod;

public class MemberData {
    private String ign;
    private String pvpType;
    private String tier;
    private String role;
    private String rank;
    private String rankColor;

    public String getIgn() {
        return ign != null ? ign : "";
    }

    public String getPvpType() {
        return pvpType != null ? pvpType : "CPvP";
    }

    public String getTier() {
        return tier != null ? tier : "";
    }

    public String getRole() {
        return role;
    }

    public String getRank() {
        return rank != null ? rank : "VK";
    }

    public String getRankColor() {
        return rankColor != null ? rankColor : "§5";
    }
}
