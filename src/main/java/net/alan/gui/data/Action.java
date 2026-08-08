package net.alan.gui.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

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
    private JsonElement card;
    @SerializedName("confirm_with") private String confirmWith;
    private Map<String, String> params;

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
    public JsonElement getCard() { return card; }
    public void setCard(JsonElement card) { this.card = card; }
    public String getConfirmWith() { return confirmWith; }
    public void setConfirmWith(String confirmWith) { this.confirmWith = confirmWith; }
    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }

    public String getCardId() {
        if (card instanceof JsonPrimitive prim && prim.isString()) {
            return prim.getAsString();
        }
        return null;
    }

    public JsonObject getCardObject() {
        if (card instanceof JsonObject obj) {
            return obj;
        }
        return null;
    }
}