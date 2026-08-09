package net.alan.gui.render;

import net.alan.gui.data.Action;
import net.alan.gui.data.source.PackDataSource;
import net.alan.gui.data.source.GameRulesBridge;
import net.alan.gui.data.source.DataPackBridge;
import net.alan.gui.data.source.ServerListDataSource;
import net.alan.gui.registry.ScreenRegistry;
import net.alan.gui.render.card.CardManager;
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
    private final Map<String, net.alan.gui.widget.BoxWidget> boxRegistry = new HashMap<>();
    private final List<Runnable> refreshCallbacks = new ArrayList<>();
    private CardManager cardManager;

    private static final Map<String, String> CONFIRM_CARDS = Map.of(
        "delete_server", "sirius_ui:cards/delete_confirm",
        "delete_world", "sirius_ui:cards/delete_confirm"
    );

    public ActionExecutor(Minecraft minecraft, Screen parentScreen) {
        this.minecraft = minecraft;
        this.parentScreen = parentScreen;
        this.sharedState = new HashMap<>();
        LOGGER.debug("ActionExecutor created with parentScreen: {}", parentScreen);
    }

    public void setCardManager(CardManager cardManager) {
        this.cardManager = cardManager;
    }

    public CardManager getCardManager() {
        return cardManager;
    }

    public void addRefreshCallback(Runnable callback) {
        refreshCallbacks.add(callback);
    }

    private void refreshAllDynamicLists() {
        for (Runnable cb : refreshCallbacks) {
            cb.run();
        }
    }

    public void registerBox(String boxId, net.alan.gui.widget.BoxWidget box) {
        boxRegistry.put(boxId, box);
        LOGGER.debug("Registered BoxWidget: {}", boxId);
    }

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

        if (action.getConfirmWith() == null && CONFIRM_CARDS.containsKey(type)) {
            action.setConfirmWith(CONFIRM_CARDS.get(type));
        }
        if (action.getConfirmWith() != null && !action.getConfirmWith().isBlank()) {
            showConfirmCard(action);
            return;
        }

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
            case "show_card" -> {
                JsonElement cardEl = action.getCard();
                if (cardEl == null) {
                    LOGGER.warn("show_card action missing card");
                    return;
                }
                Map<String, String> params = action.getParams();
                if (params == null) params = Map.of();
                String instanceId = "card_" + System.currentTimeMillis();
                if (cardManager != null) {
                    String cardId = action.getCardId();
                    JsonObject cardObj = action.getCardObject();
                    if (cardId != null) {
                        cardManager.showCard(cardId, instanceId, params);
                    } else if (cardObj != null) {
                        cardManager.showCardFromJson(cardObj, instanceId, params);
                    } else {
                        LOGGER.warn("show_card action has invalid card type");
                    }
                }
            }
            case "dismiss_card" -> {
                if (cardManager != null) {
                    cardManager.dismissAll();
                }
            }
            case "card_submit" -> {
                if (cardManager != null) {
                    Action pending = cardManager.getTopPendingAction();
                    cardManager.dismissAll();
                    if (pending != null) {
                        execute(pending);
                    }
                }
            }
            case "card_cancel" -> {
                if (cardManager != null) {
                    cardManager.dismissAll();
                }
            }
            case "show_toast" -> {
                if (cardManager != null) {
                    String message = action.getContent() != null
                            ? action.getContent()
                            : Component.translatable("gui.toast.message").getString();
                    int duration = 3000;
                    if (action.getTarget() != null) {
                        try {
                            duration = Integer.parseInt(action.getTarget());
                        } catch (NumberFormatException ignored) {}
                    }
                    cardManager.showToast("toast_" + System.currentTimeMillis(), message, duration);
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
                    Screen targetScreen = parentScreen;
                    if (parentScreen instanceof net.alan.gui.screen.JsonScreen jsonScreen) {
                        try {
                            var field = net.alan.gui.screen.JsonScreen.class.getDeclaredField("lastScreen");
                            field.setAccessible(true);
                            Screen lastScreen = (Screen) field.get(jsonScreen);
                            if (lastScreen != null) {
                                targetScreen = lastScreen;
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to get lastScreen from JsonScreen", e);
                        }
                    } else {
                        Screen parent = getParentFromScreen(parentScreen);
                        if (parent != null) {
                            targetScreen = parent;
                        }
                    }
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
                    LOGGER.info("Opening link: {}", action.getUrl());
                    ConfirmLinkScreen.confirmLinkNow(action.getUrl(), parentScreen, false);
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
                minecraft.createWorldOpenFlows().loadLevel(parentScreen, levelId);
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
                        ConnectScreen.startConnecting(parentScreen, minecraft, address, serverData, false);
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
                LevelStorageSource levelSource = minecraft.getLevelSource();
                try (LevelStorageSource.LevelStorageAccess access = levelSource.createAccess(levelId)) {
                    access.deleteLevel();
                    LOGGER.info("World deleted: {}", levelId);
                } catch (IOException e) {
                    LOGGER.error("Failed to delete world: {}", levelId, e);
                    SystemToast.onWorldDeleteFailure(minecraft, levelId);
                }
                refreshAllDynamicLists();
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
                try {
                    LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource()
                            .validateAndCreateAccess(levelId);
                    minecraft.setScreen(new EditWorldScreen(confirmed -> {
                        try {
                            access.close();
                        } catch (IOException ignored) {
                        }
                        refreshAllDynamicLists();
                        minecraft.setScreen(parentScreen);
                    }, access));
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
                    // worldCreationContext.validate(); // not available in 1.20.1
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
                    access.close();
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
                        LOGGER.info("Showing edit server card: {} ({})", serverData.name, serverData.ip);
                        if (cardManager != null) {
                            Map<String, String> params = new LinkedHashMap<>();
                            params.put("title", I18n.get("selectServer.edit"));
                            params.put("confirm_text", CommonComponents.GUI_DONE.getString());
                            params.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                            params.put("name", serverData.name);
                            params.put("address", serverData.ip);
                            params.put("server_index", indexStr);
                            cardManager.showCard("sirius_ui:cards/server_input",
                                    "edit_server_card", params);
                        }
                    } else {
                        LOGGER.warn("Invalid server index: {}", index);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid server index: {}", indexStr, e);
                }
            }
            case "edit_server_submit", "server_input_submit" -> {
                String indexStr = cardManager != null ?
                        cardManager.getTopCardParam("server_index") : null;
                String name = cardManager != null ?
                        cardManager.getInputValue("edit_server_card", "name") : null;
                if (name == null) {
                    name = cardManager != null ?
                            cardManager.getInputValue("add_server_card", "name") : null;
                }
                String address = cardManager != null ?
                        cardManager.getInputValue("edit_server_card", "address") : null;
                if (address == null) {
                    address = cardManager != null ?
                            cardManager.getInputValue("add_server_card", "address") : null;
                }
                if (name == null || name.isBlank()) {
                    name = I18n.get("selectServer.defaultName");
                }
                if (address == null || address.isBlank()) {
                    LOGGER.warn("Server input address is empty");
                    return;
                }
                ServerList serverList = new ServerList(minecraft);
                serverList.load();
                if (indexStr != null) {
                    try {
                        int index = Integer.parseInt(indexStr);
                        if (index >= 0 && index < serverList.size()) {
                            ServerData serverData = serverList.get(index);
                            serverData.name = name;
                            serverData.ip = address;
                            serverList.save();
                            LOGGER.info("Edited server: {} ({})", serverData.name, serverData.ip);
                            refreshAllDynamicLists();
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.error("Invalid server index: {}", indexStr, e);
                    }
                } else {
                    ServerData newServer = new ServerData(name, address, false);
                    serverList.add(newServer, false);
                    serverList.save();
                    LOGGER.info("Added server: {} ({})", name, address);
                    refreshAllDynamicLists();
                }
                if (cardManager != null) cardManager.dismissAll();
            }
            case "open_folder" -> {
                String target = action.getTarget();
                if (target == null) {
                    LOGGER.warn("open_folder action missing target");
                    return;
                }
                if ("resourcepacks".equals(target)) {
                    LOGGER.info("Opening resource packs folder");
                    Util.getPlatform().openFile(minecraft.getResourcePackDirectory().toFile());
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
                        worldName, levelSettings, worldOptions, dimensionGetter);
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
                LOGGER.info("Showing direct connect card");
                if (cardManager != null) {
                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("title", I18n.get("selectServer.direct"));
                    params.put("confirm_text", CommonComponents.GUI_PROCEED.getString());
                    params.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                    cardManager.showCard("sirius_ui:cards/direct_connect",
                            "direct_connect_card", params);
                }
            }
            case "direct_connect_submit" -> {
                String address = cardManager != null ?
                        cardManager.getInputValue("direct_connect_card", "address") : null;
                if (address == null || address.isBlank()) {
                    LOGGER.warn("Direct connect address is empty");
                    return;
                }
                LOGGER.info("Direct connecting to: {}", address);
                ServerAddress serverAddress = ServerAddress.parseString(address);
                ServerData serverData = new ServerData(
                        I18n.get("selectServer.defaultName"), address, false);
                ConnectScreen.startConnecting(parentScreen, minecraft, serverAddress, serverData, false);
                if (cardManager != null) cardManager.dismissAll();
            }
            case "add_server" -> {
                LOGGER.info("Showing add server card");
                if (cardManager != null) {
                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("title", I18n.get("selectServer.add"));
                    params.put("confirm_text", CommonComponents.GUI_DONE.getString());
                    params.put("cancel_text", CommonComponents.GUI_CANCEL.getString());
                    params.put("name", I18n.get("selectServer.defaultName"));
                    cardManager.showCard("sirius_ui:cards/server_input",
                            "add_server_card", params);
                }
            }
            case "add_server_submit" -> {
                Action resolved = new Action();
                resolved.setType("server_input_submit");
                execute(resolved);
            }
            case "refresh_servers" -> {
                LOGGER.info("Refreshing server list");
                ServerListDataSource.pingServers(() -> refreshAllDynamicLists());
            }
            default -> LOGGER.warn("Unknown action type: {}", type);
        }
    }

    private void showConfirmCard(Action action) {
        String cardId = action.getConfirmWith();
        String instanceId = "card_confirm_" + action.getType() + "_" + System.currentTimeMillis();

        Map<String, String> context = new LinkedHashMap<>();
        context.put("action_type", action.getType());
        if (action.getTarget() != null) context.put("target", action.getTarget());
        if (action.getContent() != null) context.put("content", action.getContent());
        if (action.getParams() != null) context.putAll(action.getParams());

        Action pendingAction = new Action();
        pendingAction.setType(action.getType());
        pendingAction.setTarget(action.getTarget());
        pendingAction.setContent(action.getContent());
        pendingAction.setScreenId(action.getScreenId());
        pendingAction.setUrl(action.getUrl());
        pendingAction.setVarName(action.getVarName());
        pendingAction.setVarValue(action.getVarValue());
        pendingAction.setBoxId(action.getBoxId());
        pendingAction.setTargetId(action.getTargetId());
        pendingAction.setParams(action.getParams());

        if (cardManager != null) {
            cardManager.showCardWithConfirm(cardId, instanceId, context, pendingAction);
        }
    }

    private void disconnectAndGoToTitle() {
        boolean isLocal = minecraft.isLocalServer();
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        if (isLocal) {
            minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("connect.joining")));
        }
        TitleScreen titleScreen = new TitleScreen();
        minecraft.setScreen(titleScreen);
        LOGGER.info("Disconnected: isLocal={}", isLocal);
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