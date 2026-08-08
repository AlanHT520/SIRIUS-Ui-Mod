package net.alan.gui.data.source;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeleteConfirmDataSource implements CardDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteConfirmDataSource.class);

    @Override
    public Map<String, String> load(Map<String, String> context) {
        Map<String, String> result = new LinkedHashMap<>();
        String actionType = context.get("action_type");

        if ("delete_server".equals(actionType)) {
            String serverName = context.get("content");
            if (serverName == null || serverName.isBlank()) {
                serverName = "Unknown";
            }
            result.put("title", Component.translatable("selectServer.deleteQuestion").getString());
            result.put("message", Component.translatable("selectServer.deleteWarning", serverName).getString());
            result.put("confirm_text", Component.translatable("selectServer.deleteButton").getString());
            result.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
        } else if ("delete_world".equals(actionType)) {
            String worldName = context.get("content");
            if (worldName == null || worldName.isBlank()) {
                worldName = context.get("target");
            }
            result.put("title", Component.translatable("selectWorld.deleteQuestion").getString());
            result.put("message", Component.translatable("selectWorld.deleteWarning", worldName).getString());
            result.put("confirm_text", Component.translatable("selectWorld.deleteButton").getString());
            result.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
        } else {
            result.put("title", "Confirm");
            result.put("message", "Are you sure?");
            result.put("confirm_text", "OK");
            result.put("cancel_text", "Cancel");
        }

        return result;
    }
}