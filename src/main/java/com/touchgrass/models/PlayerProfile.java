package com.touchgrass.models;

public class PlayerProfile {
    private String profileId;
    private String accountId;
    private String avatarUrl;
    private int totalGamesPlayed;
    private boolean online;

    public PlayerProfile() {
        this.totalGamesPlayed = 0;
        this.online = false;
    }

    public PlayerProfile(String profileId, String accountId, String avatarUrl, int totalGamesPlayed, boolean online) {
        this.profileId = profileId;
        this.accountId = accountId;
        this.avatarUrl = avatarUrl;
        this.totalGamesPlayed = totalGamesPlayed;
        this.online = online;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }

    public void setTotalGamesPlayed(int totalGamesPlayed) {
        this.totalGamesPlayed = totalGamesPlayed;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
