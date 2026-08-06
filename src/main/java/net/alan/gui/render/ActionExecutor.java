package net.alan.gui.render;

import net.alan.gui.data.Action;
import net.alan.gui.data.source.PackDataSource;
import net.alan.gui.data.source.GameRulesBridge;
import net.alan.gui.data.source.DataPackBridge;
import net.alan.gui.data.source.ServerListDataSource;
import net.alan.gui.registry.ScreenRegistry;
import net.alan.gui.render.popup.PopupManager;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ActionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionExecutor.class);
    private final Minecraft minecraft;
    private final Screen parentScreen;
    private Map<String, String> sharedState;
    private final Map<String, net.alan.gui.widget.BoxWidget> boxRegistry;
    private final List<Runnable> refreshCallbacks = new ArrayList<>();
    private PopupManager popupManager;

    public ActionExecutor(Minecraft minecraft, Screen parentScreen) {
        this.minecraft = minecraft;
        this.parentScreen = parentScreen;
        this.sharedState = new HashMap<>();
        this.boxRegistry = new HashMap<>();
        LOGGER.debug("ActionExecutor created with parentScreen: {}", parentScreen);
    }

    public void setPopupManager(PopupManager popupManager) {
        this.popupManager = popupManager;
    }

    public PopupManager getPopupManager() {
        return popupManager;
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
                if ("create_world".equals(screenId)) {
                    GameRulesBridge.reset();
                    DataPackBridge.initialize();
                }
                Screen s = ScreenRegistry.openScreen(screenId, parentScreen);
                if (s != null) {
                    minecraft.setScreen(s);
                    LOGGER.info("Opened screen: {}", screenId);
                } else {
                    LOGGER.error("Cannot open screen: {}", screenId);
                }
            }
            case "show_popup" -> {
                JsonElement popupEl = action.getPopup();
                if (popupEl == null) {
                    LOGGER.warn("show_popup action missing popup");
                    return;
                }
                Map<String, String> params = action.getParams();
                if (params == null) params = Map.of();
                String instanceId = "popup_" + System.currentTimeMillis();
                if (popupManager != null) {
                    String popupId = action.getPopupId();
                    JsonObject popupObj = action.getPopupObject();
                    if (popupId != null) {
                        popupManager.showPopup(popupId, instanceId, params);
                    } else if (popupObj != null) {
                        popupManager.showPopupFromJson(popupObj, instanceId, params);
                    } else {
                        LOGGER.warn("show_popup action has invalid popup type");
                    }
                }
            }
            case "dismiss_popup" -> {
                if (popupManager != null) {
                    popupManager.dismissAll();
                }
            }
            case "show_toast" -> {
                if (popupManager != null) {
                    String message = action.getContent() != null
                            ? action.getContent()
                            : Component.translatable("gui.toast.message").getString();
                    int duration = 3000;
                    if (action.getTarget() != null) {
                        try {
                            duration = Integer.parseInt(action.getTarget());
                        } catch (NumberFormatException ignored) {}
                    }
                    popupManager.showToast("toast_" + System.currentTimeMillis(), message, duration);
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
                    // 修复：如??parentScreen ??JsonScreen，返回到它的 lastScreen
                    // 这样子屏幕（??LanguageSelectScreen）会返回??options_screen.json
                    // ??options_screen.json ??Done 按钮会返回到 PauseScreen/TitleScreen
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
                String worldName = action.getContent() != null ? action.getContent() : levelId;
                LOGGER.info("Showing delete confirmation for world: {}", levelId);
                sharedState.put("pendingDeleteLevelId", levelId);
                sharedState.put("pendingDeleteWorldName", worldName);
                if (popupManager != null) {
                    Map<String, String> popupParams = new LinkedHashMap<>();
                    popupParams.put("title", Component.translatable("selectWorld.deleteQuestion").getString());
                    popupParams.put("message", Component.translatable("selectWorld.deleteWarning", worldName).getString());
                    popupParams.put("confirm_text", Component.translatable("selectWorld.deleteButton").getString());
                    popupParams.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                    popupParams.put("confirm_action", "_do_delete_world");
                    popupManager.showPopup("sirius_ui:popups/delete_confirm",
                            "delete_world_popup", popupParams);
                }
            }
            case "_do_delete_world" -> {
                String levelId = sharedState.get("pendingDeleteLevelId");
                if (levelId == null) {
                    LOGGER.warn("_do_delete_world action missing levelId");
                    return;
                }
                LevelStorageSource levelSource = minecraft.getLevelSource();
                try (LevelStorageSource.LevelStorageAccess access = levelSource.createAccess(levelId)) {
                    access.deleteLevel();
                    LOGGER.info("World deleted: {}", levelId);
                } catch (IOException e) {
                    LOGGER.error("Failed to delete world: {}", levelId, e);
                    SystemToast.onWorldDeleteFailure(minecraft, levelId);
                }
                refreshAllDynamicLists();
                sharedState.remove("pendingDeleteLevelId");
                sharedState.remove("pendingDeleteWorldName");
                if (popupManager != null) popupManager.dismissAll();
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
                        String serverName = serverData.name;
                        LOGGER.info("Showing delete confirmation for server: {} ({})", serverName, serverData.ip);
                        sharedState.put("pendingDeleteServerIndex", indexStr);
                        if (popupManager != null) {
                            Map<String, String> popupParams = new LinkedHashMap<>();
                            popupParams.put("title", Component.translatable("selectServer.deleteQuestion").getString());
                            popupParams.put("message", Component.translatable("selectServer.deleteWarning", serverName).getString());
                            popupParams.put("confirm_text", Component.translatable("selectServer.deleteButton").getString());
                            popupParams.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                            popupParams.put("confirm_action", "_do_delete_server");
                            popupManager.showPopup("sirius_ui:popups/delete_confirm",
                                    "delete_server_popup", popupParams);
                        }
                    } else {
                        LOGGER.warn("Invalid server index: {}", index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
            }
            case "_do_delete_server" -> {
                String indexStr = sharedState.get("pendingDeleteServerIndex");
                if (indexStr == null) {
                    LOGGER.warn("_do_delete_server action missing index");
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
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
                sharedState.remove("pendingDeleteServerIndex");
                if (popupManager != null) popupManager.dismissAll();
            }
            case "edit_world" -> {
                String levelId = action.getTarget();
                if (levelId == null) {
                    LOGGER.warn("edit_world action missing target (levelId)");
                    return;
                }
                LOGGER.info("Editing world: {}", levelId);
                try {
                    LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource()
                            .validateAndCreateAccess(levelId);
                    minecraft.setScreen(EditWorldScreen.create(minecraft, access, confirmed -> {
                        access.safeClose();
                        refreshAllDynamicLists();
                        minecraft.setScreen(parentScreen);
                    }));
                } catch (IOException e) {
                    LOGGER.error("Failed to access level for editing: {}", levelId, e);
                    SystemToast.onWorldAccessFailure(minecraft, levelId);
                } catch (ContentValidationException e) {
                    LOGGER.error("Invalid world data for editing: {}", levelId, e);
                    SystemToast.onWorldAccessFailure(minecraft, levelId);
                }
            }
            case "recreate_world" -> {
                String levelId = action.getTarget();
                if (levelId == null) {
                    LOGGER.warn("recreate_world action missing target (levelId)");
                    return;
                }
                LOGGER.info("Recreating world: {}", levelId);
                try {
                    LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource()
                            .validateAndCreateAccess(levelId);
                    var pair = minecraft.createWorldOpenFlows().recreateWorldData(access);
                    var levelSettings = pair.getFirst();
                    var worldCreationContext = pair.getSecond();
                    var path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(
                            access.getLevelPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR),
                            minecraft);
                    worldCreationContext.validate();
                    if (worldCreationContext.options().isOldCustomizedWorld()) {
                        minecraft.setScreen(new ConfirmScreen(
                                confirmed -> minecraft.setScreen(
                                        confirmed
                                                ? CreateWorldScreen.createFromExisting(minecraft, parentScreen,
                                                        levelSettings, worldCreationContext, path)
                                                : parentScreen),
                                Component.translatable("selectWorld.recreate.customized.title"),
                                Component.translatable("selectWorld.recreate.customized.text"),
                                CommonComponents.GUI_PROCEED,
                                CommonComponents.GUI_CANCEL));
                    } else {
                        minecraft.setScreen(CreateWorldScreen.createFromExisting(minecraft, parentScreen,
                                levelSettings, worldCreationContext, path));
                    }
                    access.safeClose();
                } catch (Exception e) {
                    LOGGER.error("Failed to recreate world: {}", levelId, e);
                    minecraft.setScreen(parentScreen);
                }
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
                        LOGGER.info("Showing edit server input dialog: {} ({})", serverData.name, serverData.ip);
                        sharedState.put("pendingEditServerIndex", indexStr);
                        if (popupManager != null) {
                            Map<String, String> popupParams = new LinkedHashMap<>();
                            popupParams.put("title", I18n.get("selectServer.edit"));
                            popupParams.put("confirm_text", CommonComponents.GUI_DONE.getString());
                            popupParams.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                            popupParams.put("confirm_action", "_do_edit_server");
                            popupParams.put("name", serverData.name);
                            popupParams.put("address", serverData.ip);
                            popupManager.showPopup("sirius_ui:popups/server_input",
                                    "edit_server_popup", popupParams);
                        }
                    } else {
                        LOGGER.warn("Invalid server index: {}", index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
            }
            case "_do_edit_server" -> {
                String indexStr = sharedState.get("pendingEditServerIndex");
                if (indexStr == null) {
                    LOGGER.warn("_do_edit_server action missing index");
                    return;
                }
                String name = popupManager != null ?
                        popupManager.getInputValue("edit_server_popup", "name") : null;
                String address = popupManager != null ?
                        popupManager.getInputValue("edit_server_popup", "address") : null;
                try {
                    int index = Integer.parseInt(indexStr);
                    ServerList serverList = new ServerList(minecraft);
                    serverList.load();
                    if (index >= 0 && index < serverList.size()) {
                        ServerData serverData = serverList.get(index);
                        if (name != null && !name.isBlank()) {
                            serverData.name = name;
                        }
                        if (address != null && !address.isBlank()) {
                            serverData.ip = address;
                        }
                        serverList.save();
                        LOGGER.info("Edited server: {} ({})", serverData.name, serverData.ip);
                        refreshAllDynamicLists();
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
                sharedState.remove("pendingEditServerIndex");
                if (popupManager != null) popupManager.dismissAll();
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
            case "open_game_rules" -> {
                LOGGER.info("Opening game rules screen");
                Screen s = ScreenRegistry.openScreen("game_rules", parentScreen);
                if (s != null) {
                    minecraft.setScreen(s);
                }
            }
            case "cycle_rule_value" -> {
                String ruleId = action.getTarget();
                if (ruleId == null) {
                    LOGGER.warn("cycle_rule_value action missing target (ruleId)");
                    return;
                }
                GameRules rules = GameRulesBridge.getCurrentRules();
                GameRules.Key<?> key = GameRulesBridge.getKey(ruleId);
                if (key != null) {
                    GameRules.Value<?> value = rules.getRule(key);
                    if (value instanceof GameRules.BooleanValue bv) {
                        bv.set(!bv.get(), null);
                    } else if (value instanceof GameRules.IntegerValue iv) {
                        int newVal = iv.get() + 1;
                        iv.set(newVal, null);
                    }
                    LOGGER.info("Cycled rule {}: {}", ruleId, value.serialize());
                }
                refreshAllDynamicLists();
            }
            case "open_data_packs" -> {
                LOGGER.info("Opening data packs screen");
                DataPackBridge.initialize();
                Screen s = ScreenRegistry.openScreen("data_packs", parentScreen);
                if (s != null) {
                    minecraft.setScreen(s);
                }
            }
            case "create_world" -> {
                String worldName = sharedState != null ? sharedState.getOrDefault("world_name", "New World") : "New World";
                String gameModeStr = sharedState != null ? sharedState.getOrDefault("game_mode", "survival") : "survival";
                String difficultyStr = sharedState != null ? sharedState.getOrDefault("difficulty", "normal") : "normal";
                String allowCheatsStr = sharedState != null ? sharedState.getOrDefault("allow_cheats", "false") : "false";
                String worldTypeStr = sharedState != null ? sharedState.getOrDefault("world_type", "default") : "default";
                String seedStr = sharedState != null ? sharedState.getOrDefault("seed", "") : "";
                String generateStructuresStr = sharedState != null ? sharedState.getOrDefault("generate_structures", "true") : "true";
                String bonusChestStr = sharedState != null ? sharedState.getOrDefault("bonus_chest", "false") : "false";
                String allowExperimentsStr = sharedState != null ? sharedState.getOrDefault("allow_experiments", "false") : "false";

                boolean hardcore = "hardcore".equals(gameModeStr);
                GameType gameType = hardcore ? GameType.SURVIVAL : GameType.byName(gameModeStr);
                if (gameType == null) gameType = GameType.SURVIVAL;

                Difficulty difficulty = Difficulty.byName(difficultyStr);
                if (difficulty == null) difficulty = Difficulty.NORMAL;
                if (hardcore) difficulty = Difficulty.HARD;

                boolean allowCommands = hardcore ? false : "true".equals(allowCheatsStr);

                OptionalLong seedOpt = WorldOptions.parseSeed(seedStr);
                long seed = seedOpt.isPresent() ? seedOpt.getAsLong() : WorldOptions.randomSeed();

                boolean generateStructures = "true".equals(generateStructuresStr);
                boolean bonusChest = "true".equals(bonusChestStr);

                boolean allowExperiments = "true".equals(allowExperimentsStr);

                GameRules gameRules = GameRulesBridge.getCurrentRules();

                WorldDataConfiguration dataConfig = DataPackBridge.getDataConfiguration();

                LevelSettings levelSettings = new LevelSettings(
                    worldName,
                    gameType,
                    hardcore,
                    difficulty,
                    allowCommands,
                    gameRules,
                    dataConfig
                );

                WorldOptions worldOptions = new WorldOptions(seed, generateStructures, bonusChest);

                Function<RegistryAccess, WorldDimensions> dimensionGetter = registryAccess -> {
                    Registry<WorldPreset> presetRegistry = registryAccess.registryOrThrow(Registries.WORLD_PRESET);
                    ResourceKey<WorldPreset> presetKey = switch (worldTypeStr) {
                        case "flat" -> WorldPresets.FLAT;
                        case "large_biomes" -> WorldPresets.LARGE_BIOMES;
                        case "amplified" -> WorldPresets.AMPLIFIED;
                        case "single_biome_surface" -> WorldPresets.SINGLE_BIOME_SURFACE;
                        default -> WorldPresets.NORMAL;
                    };
                    WorldPreset preset = presetRegistry.get(presetKey);
                    if (preset != null) {
                        return preset.createWorldDimensions();
                    }
                    return presetRegistry.get(WorldPresets.NORMAL).createWorldDimensions();
                };

                LOGGER.info("Creating world: {} (mode={}, difficulty={}, hardcore={}, type={})",
                    worldName, gameModeStr, difficultyStr, hardcore, worldTypeStr);

                Runnable doCreate = () -> {
                    DataPackBridge.cleanup();
                    minecraft.createWorldOpenFlows().createFreshLevel(
                        worldName, levelSettings, worldOptions, dimensionGetter, parentScreen);
                };

                if (allowExperiments) {
                    minecraft.setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) {
                                doCreate.run();
                            } else {
                                minecraft.setScreen(parentScreen);
                            }
                        },
                        Component.translatable("selectWorld.warning.experimental.title"),
                        Component.translatable("selectWorld.warning.experimental.question"),
                        CommonComponents.GUI_PROCEED,
                        CommonComponents.GUI_CANCEL
                    ));
                } else {
                    doCreate.run();
                }
            }
            case "direct_connect" -> {
                LOGGER.info("Showing direct connect input dialog");
                if (popupManager != null) {
                    Map<String, String> popupParams = new LinkedHashMap<>();
                    popupParams.put("title", I18n.get("selectServer.direct"));
                    popupParams.put("confirm_text", CommonComponents.GUI_PROCEED.getString());
                    popupParams.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                    popupParams.put("confirm_action", "_do_direct_connect");
                    popupManager.showPopup("sirius_ui:popups/direct_connect",
                            "direct_connect_popup", popupParams);
                }
            }
            case "_do_direct_connect" -> {
                String address = popupManager != null ?
                        popupManager.getInputValue("direct_connect_popup", "address") : null;
                if (address == null || address.isBlank()) {
                    LOGGER.warn("Direct connect address is empty");
                    return;
                }
                LOGGER.info("Direct connecting to: {}", address);
                ServerAddress serverAddress = ServerAddress.parseString(address);
                ServerData serverData = new ServerData(
                        I18n.get("selectServer.defaultName"), address, ServerData.Type.OTHER);
                ConnectScreen.startConnecting(parentScreen, minecraft, serverAddress, serverData, false, null);
                if (popupManager != null) popupManager.dismissAll();
            }
            case "add_server" -> {
                LOGGER.info("Showing add server input dialog");
                if (popupManager != null) {
                    Map<String, String> popupParams = new LinkedHashMap<>();
                    popupParams.put("title", I18n.get("selectServer.add"));
                    popupParams.put("confirm_text", CommonComponents.GUI_DONE.getString());
                    popupParams.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                    popupParams.put("confirm_action", "_do_add_server");
                    popupParams.put("name", I18n.get("selectServer.defaultName"));
                    popupManager.showPopup("sirius_ui:popups/server_input",
                            "add_server_popup", popupParams);
                }
            }
            case "_do_add_server" -> {
                String name = popupManager != null ?
                        popupManager.getInputValue("add_server_popup", "name") : null;
                String address = popupManager != null ?
                        popupManager.getInputValue("add_server_popup", "address") : null;
                if (name == null || name.isBlank()) {
                    name = I18n.get("selectServer.defaultName");
                }
                if (address == null || address.isBlank()) {
                    LOGGER.warn("Add server address is empty");
                    return;
                }
                LOGGER.info("Adding server: {} ({})", name, address);
                ServerData newServer = new ServerData(name, address, ServerData.Type.OTHER);
                ServerList serverList = new ServerList(minecraft);
                serverList.load();
                serverList.add(newServer, false);
                serverList.save();
                refreshAllDynamicLists();
                if (popupManager != null) popupManager.dismissAll();
            }
            case "refresh_servers" -> {
                LOGGER.info("Refreshing server list");
                ServerListDataSource.pingServers(() -> refreshAllDynamicLists());
            }
            default -> LOGGER.warn("Unknown action type: {}", type);
        }
    }

    private void disconnectAndGoToTitle() {
        boolean isLocal = minecraft.isLocalServer();
        ServerData serverData = minecraft.getCurrentServer();
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        if (isLocal) {
            minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.disconnect();
        }
        TitleScreen titleScreen = new TitleScreen();
        if (isLocal) {
            minecraft.setScreen(titleScreen);
        } else if (serverData != null && serverData.isRealm()) {
            minecraft.setScreen(new RealmsMainScreen(titleScreen));
        } else {
            minecraft.setScreen(new JoinMultiplayerScreen(titleScreen));
        }
        LOGGER.info("Disconnected: isLocal={}, isRealm={}", isLocal, serverData != null && serverData.isRealm());
    }

    /**
     * 获取真实的父屏幕
     * 如果 parentScreen ??JsonScreen，则递归获取它的 lastScreen
     * 这样可以确保 close_screen 动作返回到正确的父屏??
     */
    private Screen getRealParentScreen(Screen screen) {
        java.util.Set<Screen> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Screen current = screen;
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            if (current instanceof net.alan.gui.screen.JsonScreen jsonScreen) {
                try {
                    var field = net.alan.gui.screen.JsonScreen.class.getDeclaredField("lastScreen");
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