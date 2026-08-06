package net.alan.gui.data;

import javax.annotation.Nullable;

public class DynamicListData {
    private final String id;
    private final String name;
    private final String description;
    private final String iconPath;
    private final String actionType;
    private final boolean joinable;
    private final long lastPlayed;
    private final String versionInfo;
    private final String gameMode;
    private final boolean isLocked;
    private final boolean isCompatible;
    private final boolean canEdit;
    private final boolean canDelete;
    private final boolean canRecreate;
    private final boolean shouldBackup;
    private final boolean requiresManualConversion;
    private final boolean isExperimental;
    private final boolean isDisabled;
    private final boolean isDowngrade;
    private final long ping;
    private final int onlinePlayers;
    private final int maxPlayers;
    private final String motd;
    private final boolean isLan;
    private final String serverIconBase64;
    private final boolean isOnline;

    private DynamicListData(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.iconPath = builder.iconPath;
        this.actionType = builder.actionType;
        this.joinable = builder.joinable;
        this.lastPlayed = builder.lastPlayed;
        this.versionInfo = builder.versionInfo;
        this.gameMode = builder.gameMode;
        this.isLocked = builder.isLocked;
        this.isCompatible = builder.isCompatible;
        this.canEdit = builder.canEdit;
        this.canDelete = builder.canDelete;
        this.canRecreate = builder.canRecreate;
        this.shouldBackup = builder.shouldBackup;
        this.requiresManualConversion = builder.requiresManualConversion;
        this.isExperimental = builder.isExperimental;
        this.isDisabled = builder.isDisabled;
        this.isDowngrade = builder.isDowngrade;
        this.ping = builder.ping;
        this.onlinePlayers = builder.onlinePlayers;
        this.maxPlayers = builder.maxPlayers;
        this.motd = builder.motd;
        this.isLan = builder.isLan;
        this.serverIconBase64 = builder.serverIconBase64;
        this.isOnline = builder.isOnline;
    }

    public DynamicListData(String id, String name, String description,
                           String iconPath, String actionType, boolean joinable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconPath = iconPath;
        this.actionType = actionType;
        this.joinable = joinable;
        this.lastPlayed = -1L;
        this.versionInfo = "";
        this.gameMode = "";
        this.isLocked = false;
        this.isCompatible = true;
        this.canEdit = joinable;
        this.canDelete = joinable;
        this.canRecreate = joinable;
        this.shouldBackup = false;
        this.requiresManualConversion = false;
        this.isExperimental = false;
        this.isDisabled = false;
        this.isDowngrade = false;
        this.ping = -1L;
        this.onlinePlayers = 0;
        this.maxPlayers = 0;
        this.motd = "";
        this.isLan = false;
        this.serverIconBase64 = null;
        this.isOnline = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }
    public String getActionType() { return actionType; }
    public boolean isJoinable() { return joinable; }
    public long getLastPlayed() { return lastPlayed; }
    public String getVersionInfo() { return versionInfo; }
    public String getGameMode() { return gameMode; }
    public boolean isLocked() { return isLocked; }
    public boolean isCompatible() { return isCompatible; }
    public boolean canEdit() { return canEdit; }
    public boolean canDelete() { return canDelete; }
    public boolean canRecreate() { return canRecreate; }
    public boolean shouldBackup() { return shouldBackup; }
    public boolean requiresManualConversion() { return requiresManualConversion; }
    public boolean isExperimental() { return isExperimental; }
    public boolean isDisabled() { return isDisabled; }
    public boolean isDowngrade() { return isDowngrade; }
    public long getPing() { return ping; }
    public int getOnlinePlayers() { return onlinePlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getMotd() { return motd; }
    public boolean isLan() { return isLan; }
    public String getServerIconBase64() { return serverIconBase64; }
    public boolean isOnline() { return isOnline; }

    public static class Builder {
        private final String id;
        private final String name;
        private String description = "";
        private String iconPath = null;
        private String actionType = "join_world";
        private boolean joinable = true;
        private long lastPlayed = -1L;
        private String versionInfo = "";
        private String gameMode = "";
        private boolean isLocked = false;
        private boolean isCompatible = true;
        private boolean canEdit = true;
        private boolean canDelete = true;
        private boolean canRecreate = true;
        private boolean shouldBackup = false;
        private boolean requiresManualConversion = false;
        private boolean isExperimental = false;
        private boolean isDisabled = false;
        private boolean isDowngrade = false;
        private long ping = -1L;
        private int onlinePlayers = 0;
        private int maxPlayers = 0;
        private String motd = "";
        private boolean isLan = false;
        private String serverIconBase64 = null;
        private boolean isOnline = false;

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder description(String val) { this.description = val; return this; }
        public Builder iconPath(@Nullable String val) { this.iconPath = val; return this; }
        public Builder actionType(String val) { this.actionType = val; return this; }
        public Builder joinable(boolean val) { this.joinable = val; return this; }
        public Builder lastPlayed(long val) { this.lastPlayed = val; return this; }
        public Builder versionInfo(String val) { this.versionInfo = val; return this; }
        public Builder gameMode(String val) { this.gameMode = val; return this; }
        public Builder isLocked(boolean val) { this.isLocked = val; return this; }
        public Builder isCompatible(boolean val) { this.isCompatible = val; return this; }
        public Builder canEdit(boolean val) { this.canEdit = val; return this; }
        public Builder canDelete(boolean val) { this.canDelete = val; return this; }
        public Builder canRecreate(boolean val) { this.canRecreate = val; return this; }
        public Builder shouldBackup(boolean val) { this.shouldBackup = val; return this; }
        public Builder requiresManualConversion(boolean val) { this.requiresManualConversion = val; return this; }
        public Builder isExperimental(boolean val) { this.isExperimental = val; return this; }
        public Builder isDisabled(boolean val) { this.isDisabled = val; return this; }
        public Builder isDowngrade(boolean val) { this.isDowngrade = val; return this; }
        public Builder ping(long val) { this.ping = val; return this; }
        public Builder onlinePlayers(int val) { this.onlinePlayers = val; return this; }
        public Builder maxPlayers(int val) { this.maxPlayers = val; return this; }
        public Builder motd(String val) { this.motd = val; return this; }
        public Builder isLan(boolean val) { this.isLan = val; return this; }
        public Builder serverIconBase64(@Nullable String val) { this.serverIconBase64 = val; return this; }
        public Builder isOnline(boolean val) { this.isOnline = val; return this; }

        public DynamicListData build() {
            return new DynamicListData(this);
        }
    }
}