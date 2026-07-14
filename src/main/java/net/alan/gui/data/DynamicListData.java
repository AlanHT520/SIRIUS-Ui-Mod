package net.alan.gui.data;

public class DynamicListData {
    private final String id;
    private final String name;
    private final String description;
    private final String iconPath;
    private final String actionType;
    private final boolean joinable;

    public DynamicListData(String id, String name, String description,
                           String iconPath, String actionType, boolean joinable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconPath = iconPath;
        this.actionType = actionType;
        this.joinable = joinable;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }
    public String getActionType() { return actionType; }
    public boolean isJoinable() { return joinable; }
}