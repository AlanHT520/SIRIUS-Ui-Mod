package net.alan.gui.render;

import net.alan.gui.data.action.Action;
import net.alan.gui.data.source.PackDataSource;
import net.alan.gui.registry.ScreenRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionExecutor.class);
    private final Minecraft minecraft;
    private final Screen parentScreen;
    private Map<String, String> sharedState;
    private final Map<String, net.alan.gui.widget.BoxWidget> boxRegistry;
    private final List<Runnable> refreshCallbacks = new ArrayList<>();

    public ActionExecutor(Minecraft minecraft, Screen parentScreen) {
        this.minecraft = minecraft;
        this.parentScreen = parentScreen;
        this.sharedState = new HashMap<>();
        this.boxRegistry = new HashMap<>();
        LOGGER.debug("ActionExecutor created with parentScreen: {}", parentScreen);
    }

    public void addRefreshCallback(Runnable callback) {
        refreshCallbacks.add(callback);
    }

    private void refreshAllDynamicLists() {
        for (Runnable cb : refreshCallbacks) {
            cb.run();
        }
    }

    /**
     * 注册 BoxWidget 到注册表，供 switch_box 动作使用
     */
    public void registerBox(String boxId, net.alan.gui.widget.BoxWidget box) {
        boxRegistry.put(boxId, box);
        LOGGER.debug("Registered BoxWidget: {}", boxId);
    }

    /**
     * 根据 boxId 获取 BoxWidget
     */
    public net.alan.gui.widget.BoxWidget getBox(String boxId) {
        return boxRegistry.get(boxId);
    }

    public void setSharedState(Map<String, String> sharedState) {
        this.sharedState = sharedState;
    }

    public Map<String, String> getSharedState() {
        return sharedState;
    }

    public void execute(Action action) {
        if (action == null) {
            LOGGER.warn("Action is null, cannot execute.");
            return;
        }
        String type = action.getType();
        LOGGER.info("Executing action: type='{}', action={}", type, action);

        switch (type) {
            case "open_screen" -> {
                String screenId = action.getScreenId();
                if (screenId == null) {
                    LOGGER.warn("open_screen action missing screenId");
                    return;
                }
                Screen s = ScreenRegistry.openScreen(screenId, parentScreen);
                if (s != null) {
                    minecraft.setScreen(s);
                    LOGGER.info("Opened screen: {}", screenId);
                } else {
                    LOGGER.error("Cannot open screen: {}", screenId);
                }
            }
            case "quit_game" -> {
                LOGGER.info("Quitting game");
                minecraft.stop();
            }
            case "close_screen" -> {
                LOGGER.info("Closing screen, parentScreen={}", parentScreen);
                PackDataSource.commitAll();
                if (parentScreen != null) {
                    // 修复：如果 parentScreen 是 JsonScreen，返回到它的 lastScreen
                    // 这样子屏幕（如 LanguageSelectScreen）会返回到 options_screen.json
                    // 而 options_screen.json 的 Done 按钮会返回到 PauseScreen/TitleScreen
                    Screen targetScreen = getRealParentScreen(parentScreen);
                    minecraft.setScreen(targetScreen);
                    LOGGER.info("Returned to parent screen: {}", targetScreen);
                } else {
                    LOGGER.warn("parentScreen is null, returning to title screen");
                    minecraft.setScreen(new TitleScreen());
                }
            }
            case "resume_game" -> {
                LOGGER.info("Resuming game");
                minecraft.setScreen(null);
                minecraft.mouseHandler.grabMouse();
            }
            case "disconnect", "exit_to_title" -> disconnectAndGoToTitle();
            case "open_link" -> {
                try {
                    LOGGER.info("Opening link: {}", action.getUrl());
                    ConfirmLinkScreen.confirmLinkNow(parentScreen, new URI(action.getUrl()));
                } catch (URISyntaxException e) {
                    LOGGER.error("Invalid URL: {}", action.getUrl(), e);
                }
            }
            case "respawn" -> {
                if (minecraft.player != null && minecraft.player.isDeadOrDying()) {
                    LOGGER.info("Respawning player.json");
                    minecraft.player.respawn();
                    minecraft.setScreen(null);
                } else {
                    LOGGER.warn("Cannot respawn: player.json is null or not dead");
                }
            }
            case "set_var" -> {
                String varName = action.getVarName();
                String varValue = action.getVarValue();
                if (varName != null && varValue != null && sharedState != null) {
                    sharedState.put(varName, varValue);
                    LOGGER.info("set_var: {} = {}", varName, varValue);
                } else {
                    LOGGER.warn("set_var action missing varName/varValue or sharedState is null");
                }
            }
            case "switch_box" -> {
                String boxId = action.getBoxId();
                String targetId = action.getTargetId();
                if (boxId != null && targetId != null) {
                    net.alan.gui.widget.BoxWidget box = boxRegistry.get(boxId);
                    if (box != null) {
                        boolean success = box.switchTo(targetId);
                        if (success) {
                            LOGGER.info("switch_box: {} -> {}", boxId, targetId);
                        } else {
                            LOGGER.warn("switch_box failed: box '{}' has no element '{}'", boxId, targetId);
                        }
                    } else {
                        LOGGER.warn("switch_box: BoxWidget '{}' not found in registry", boxId);
                    }
                } else {
                    LOGGER.warn("switch_box action missing boxId/targetId");
                }
            }
            case "join_world" -> {
                String levelId = action.getTarget();
                if (levelId == null) {
                    LOGGER.warn("join_world action missing target (levelId)");
                    return;
                }
                LOGGER.info("Joining world: {}", levelId);
                minecraft.createWorldOpenFlows().openWorld(levelId, () -> {});
            }
            case "join_server" -> {
                String indexStr = action.getTarget();
                if (indexStr == null) {
                    LOGGER.warn("join_server action missing target (index)");
                    return;
                }
                try {
                    int index = Integer.parseInt(indexStr);
                    ServerList serverList = new ServerList(minecraft);
                    serverList.load();
                    if (index >= 0 && index < serverList.size()) {
                        ServerData serverData = serverList.get(index);
                        LOGGER.info("Joining server: {} ({})", serverData.name, serverData.ip);
                        ServerAddress address = ServerAddress.parseString(serverData.ip);
                        ConnectScreen.startConnecting(parentScreen, minecraft, address, serverData, false, null);
                    } else {
                        LOGGER.warn("Invalid server index: {}", index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
            }
            case "delete_world" -> {
                String levelId = action.getTarget();
                if (levelId == null) {
                    LOGGER.warn("delete_world action missing target (levelId)");
                    return;
                }
                LOGGER.info("Deleting world: {}", levelId);
                LevelStorageSource levelSource = minecraft.getLevelSource();
                try (LevelStorageSource.LevelStorageAccess access = levelSource.createAccess(levelId)) {
                    access.deleteLevel();
                    LOGGER.info("World deleted: {}", levelId);
                    refreshAllDynamicLists();
                } catch (IOException e) {
                    LOGGER.error("Failed to delete world: {}", levelId, e);
                    SystemToast.onWorldDeleteFailure(minecraft, levelId);
                }
            }
            case "delete_server" -> {
                String indexStr = action.getTarget();
                if (indexStr == null) {
                    LOGGER.warn("delete_server action missing target (index)");
                    return;
                }
                try {
                    int index = Integer.parseInt(indexStr);
                    ServerList serverList = new ServerList(minecraft);
                    serverList.load();
                    if (index >= 0 && index < serverList.size()) {
                        ServerData serverData = serverList.get(index);
                        serverList.remove(serverData);
                        serverList.save();
                        LOGGER.info("Deleted server: {} ({})", serverData.name, serverData.ip);
                        refreshAllDynamicLists();
                    } else {
                        LOGGER.warn("Invalid server index: {}", index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
            }
            case "edit_world" -> {
                String levelId = action.getTarget();
                if (levelId == null) {
                    LOGGER.warn("edit_world action missing target (levelId)");
                    return;
                }
                LOGGER.info("Editing world: {}", levelId);
                minecraft.setScreen(new SelectWorldScreen(parentScreen));
            }
            case "edit_server" -> {
                String indexStr = action.getTarget();
                if (indexStr == null) {
                    LOGGER.warn("edit_server action missing target (index)");
                    return;
                }
                try {
                    int index = Integer.parseInt(indexStr);
                    ServerList serverList = new ServerList(minecraft);
                    serverList.load();
                    if (index >= 0 && index < serverList.size()) {
                        ServerData serverData = serverList.get(index);
                        LOGGER.info("Editing server: {} ({})", serverData.name, serverData.ip);
                        minecraft.setScreen(new EditServerScreen(parentScreen, (BooleanConsumer) confirmed -> {
                            if (confirmed) {
                                serverList.save();
                                refreshAllDynamicLists();
                            }
                            minecraft.setScreen(parentScreen);
                        }, serverData));
                    } else {
                        LOGGER.warn("Invalid server index: {}", index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
            }
            case "open_folder" -> {
                String target = action.getTarget();
                if (target == null) {
                    LOGGER.warn("open_folder action missing target");
                    return;
                }
                if ("resourcepacks".equals(target)) {
                    LOGGER.info("Opening resource packs folder");
                    Util.getPlatform().openPath(minecraft.getResourcePackDirectory());
                } else if ("screenshots".equals(target)) {
                    LOGGER.info("Opening screenshots folder");
                    Util.getPlatform().openFile(minecraft.gameDirectory);
                } else {
                    LOGGER.warn("Unknown folder target: {}", target);
                }
            }
            case "commit_packs" -> {
                LOGGER.info("Committing resource pack changes");
                PackDataSource.commitAll();
            }
            default -> LOGGER.warn("Unknown action type: {}", type);
        }
    }

    private void disconnectAndGoToTitle() {
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        minecraft.setScreen(new TitleScreen());
        LOGGER.info("Disconnected and went to title screen");
    }

    /**
     * 获取真实的父屏幕
     * 如果 parentScreen 是 JsonScreen，则递归获取它的 lastScreen
     * 这样可以确保 close_screen 动作返回到正确的父屏幕
     */
    private Screen getRealParentScreen(Screen screen) {
        java.util.Set<Screen> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Screen current = screen;
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            if (current instanceof net.alan.gui.elements.JsonScreen jsonScreen) {
                try {
                    var field = net.alan.gui.elements.JsonScreen.class.getDeclaredField("lastScreen");
                    field.setAccessible(true);
                    Screen lastScreen = (Screen) field.get(jsonScreen);
                    if (lastScreen != null) {
                        current = lastScreen;
                        continue;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to get lastScreen from JsonScreen", e);
                }
                break;
            }
            Screen parent = getParentFromScreen(current);
            if (parent != null) {
                current = parent;
                continue;
            }
            break;
        }
        return current;
    }

    private Screen getParentFromScreen(Screen screen) {
        for (String fieldName : new String[]{"parent", "lastScreen"}) {
            try {
                var field = screen.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(screen);
                if (value instanceof Screen parent) {
                    return parent;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                LOGGER.debug("Failed to access field '{}' on {}", fieldName, screen.getClass().getSimpleName());
            }
        }
        return null;
    }
}