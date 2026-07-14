package net.alan.gui.data.action;

public class Action {
    private String type;
    private String screenId;
    private String url;
    private String target;
    private String content;
    private String varName;
    private String varValue;
    private String boxId;
    private String targetId;

    public Action() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getScreenId() { return screenId; }
    public void setScreenId(String screenId) { this.screenId = screenId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVarName() { return varName; }
    public void setVarName(String varName) { this.varName = varName; }
    public String getVarValue() { return varValue; }
    public void setVarValue(String varValue) { this.varValue = varValue; }
    public String getBoxId() { return boxId; }
    public void setBoxId(String boxId) { this.boxId = boxId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}