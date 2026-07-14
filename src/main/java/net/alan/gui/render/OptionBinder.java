package net.alan.gui.render;

import net.alan.gui.data.CycleValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;

import java.util.List;
import java.util.Map;

public class OptionBinder {

    public static void syncSlider(String optionKey, double ratio,
                                  double min, double max,
                                  Map<String, String> vars, Options opts) {
        double value = min + Mth.clamp(ratio, 0.0, 1.0) * (max - min);
        setOptionValue(optionKey, value, opts);
        vars.put("display_value", formatValue(optionKey, value));
    }

    @Deprecated
    public static void syncSlider(String optionKey, double ratio,
                                  Map<String, String> vars, Options opts) {
        double min = Double.parseDouble(vars.getOrDefault("slider_min", getDefaultMin(optionKey)));
        double max = Double.parseDouble(vars.getOrDefault("slider_max", getDefaultMax(optionKey)));
        syncSlider(optionKey, ratio, min, max, vars, opts);
    }

    public static double getOptionRawValue(String key, Options opts) {
        return switch (key) {
            case "fov" -> opts.fov().get().doubleValue();
            case "mouseSensitivity" -> opts.sensitivity().get();
            case "soundVolume" -> opts.getSoundSourceVolume(SoundSource.MASTER);
            case "musicVolume" -> opts.getSoundSourceVolume(SoundSource.MUSIC);
            case "recordVolume" -> opts.getSoundSourceVolume(SoundSource.RECORDS);
            case "weatherVolume" -> opts.getSoundSourceVolume(SoundSource.WEATHER);
            case "hostileVolume" -> opts.getSoundSourceVolume(SoundSource.HOSTILE);
            case "ambientVolume" -> opts.getSoundSourceVolume(SoundSource.AMBIENT);
            case "voiceVolume" -> opts.getSoundSourceVolume(SoundSource.VOICE);
            case "blockVolume" -> opts.getSoundSourceVolume(SoundSource.BLOCKS);
            case "playerVolume" -> opts.getSoundSourceVolume(SoundSource.PLAYERS);
            case "neutralVolume" -> opts.getSoundSourceVolume(SoundSource.NEUTRAL);
            case "renderDistance" -> opts.renderDistance().get().doubleValue();
            case "simulationDistance" -> opts.simulationDistance().get().doubleValue();
            case "maxFps" -> opts.framerateLimit().get().doubleValue();
            case "entityDistanceScaling" -> opts.entityDistanceScaling().get();
            case "menuBackgroundBlurriness" -> opts.menuBackgroundBlurriness().get().doubleValue();
            case "gamma" -> opts.gamma().get();
            case "screenEffectScale" -> opts.screenEffectScale().get();
            case "fovEffectScale" -> opts.fovEffectScale().get();
            case "glintSpeed" -> opts.glintSpeed().get();
            case "glintStrength" -> opts.glintStrength().get();
            case "mipmapLevels" -> opts.mipmapLevels().get().doubleValue();
            case "textBackgroundOpacity" -> opts.textBackgroundOpacity().get();
            case "chatOpacity" -> opts.chatOpacity().get();
            case "chatLineSpacing" -> opts.chatLineSpacing().get();
            case "chatDelay" -> opts.chatDelay().get();
            case "notificationDisplayTime" -> opts.notificationDisplayTime().get();
            case "darknessEffectScale" -> opts.darknessEffectScale().get();
            case "damageTiltStrength" -> opts.damageTiltStrength().get();
            case "panoramaSpeed" -> opts.panoramaSpeed().get();
            case "chatScale" -> opts.chatScale().get();
            case "chatWidth" -> opts.chatWidth().get();
            case "chatHeightFocused" -> opts.chatHeightFocused().get();
            case "chatHeightUnfocused" -> opts.chatHeightUnfocused().get();
            case "mouseWheelSensitivity" -> opts.mouseWheelSensitivity().get();
            default -> 0.5;
        };
    }

    public static void setOptionValue(String key, double value, Options opts) {
        switch (key) {
            case "fov" -> opts.fov().set((int) Math.round(value));
            case "mouseSensitivity" -> opts.sensitivity().set(value);
            case "soundVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.MASTER).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "musicVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.MUSIC).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "recordVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.RECORDS).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "weatherVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.WEATHER).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "hostileVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.HOSTILE).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "ambientVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "voiceVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.VOICE).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "blockVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.BLOCKS).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "playerVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.PLAYERS).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "neutralVolume" -> {
                opts.getSoundSourceOptionInstance(SoundSource.NEUTRAL).set(value);
                Minecraft.getInstance().getSoundManager().reload();
            }
            case "renderDistance" -> opts.renderDistance().set((int) Math.round(value));
            case "simulationDistance" -> opts.simulationDistance().set((int) Math.round(value));
            case "maxFps" -> {
                opts.framerateLimit().set((int) Math.round(value));
                Minecraft.getInstance().getWindow().setFramerateLimit((int) Math.round(value));
            }
            case "entityDistanceScaling" -> opts.entityDistanceScaling().set(value);
            case "menuBackgroundBlurriness" -> opts.menuBackgroundBlurriness().set((int) Math.round(value));
            case "gamma" -> opts.gamma().set(value);
            case "screenEffectScale" -> opts.screenEffectScale().set(value);
            case "fovEffectScale" -> opts.fovEffectScale().set(value);
            case "glintSpeed" -> opts.glintSpeed().set(value);
            case "glintStrength" -> opts.glintStrength().set(value);
            case "mipmapLevels" -> opts.mipmapLevels().set((int) Math.round(value));
            case "textBackgroundOpacity" -> opts.textBackgroundOpacity().set(value);
            case "chatOpacity" -> opts.chatOpacity().set(value);
            case "chatLineSpacing" -> opts.chatLineSpacing().set(value);
            case "chatDelay" -> opts.chatDelay().set(value);
            case "notificationDisplayTime" -> opts.notificationDisplayTime().set(value);
            case "darknessEffectScale" -> opts.darknessEffectScale().set(value);
            case "damageTiltStrength" -> opts.damageTiltStrength().set(value);
            case "panoramaSpeed" -> opts.panoramaSpeed().set(value);
            case "chatScale" -> opts.chatScale().set(value);
            case "chatWidth" -> opts.chatWidth().set(value);
            case "chatHeightFocused" -> opts.chatHeightFocused().set(value);
            case "chatHeightUnfocused" -> opts.chatHeightUnfocused().set(value);
            case "mouseWheelSensitivity" -> opts.mouseWheelSensitivity().set(value);
        }
    }

    public static void saveOptions(Options opts) {
        opts.save();
    }

    private static String getDefaultMin(String key) {
        return switch (key) {
            case "fov" -> "30";
            default -> "0";
        };
    }

    private static String getDefaultMax(String key) {
        return switch (key) {
            case "fov" -> "110";
            default -> "1";
        };
    }

    public static String formatValue(String key, double value) {
        return switch (key) {
            case "fov" -> {
                int v = (int) Math.round(value);
                if (v == 70) yield "options.fov.min";
                if (v == 110) yield "options.fov.max";
                yield String.valueOf(v);
            }
            case "renderDistance", "simulationDistance" -> String.valueOf((int) Math.round(value));
            case "maxFps" -> {
                int v = (int) Math.round(value);
                if (v >= 260) yield "options.framerateLimit.max";
                yield String.valueOf(v);
            }
            case "menuBackgroundBlurriness" -> value == 0 ? "options.off" : String.valueOf((int) Math.round(value));
            case "mipmapLevels" -> {
                int v = (int) Math.round(value);
                if (v == 0) yield "options.off";
                yield String.valueOf(v);
            }
            case "gamma" -> {
                int i = (int) (value * 100.0);
                if (i == 0) yield "options.gamma.min";
                if (i == 50) yield "options.gamma.default";
                if (i == 100) yield "options.gamma.max";
                yield String.valueOf(i);
            }
            case "mouseSensitivity" -> {
                if (value == 0.0) yield "options.sensitivity.min";
                if (value == 1.0) yield "options.sensitivity.max";
                yield String.format("%.0f%%", value * 200.0);
            }
            case "soundVolume", "musicVolume",
                 "recordVolume", "weatherVolume", "hostileVolume",
                 "ambientVolume", "voiceVolume", "blockVolume",
                 "playerVolume", "neutralVolume" -> String.format("%.0f%%", value * 100);
            case "textBackgroundOpacity", "chatOpacity", "chatLineSpacing",
                 "screenEffectScale", "fovEffectScale", "darknessEffectScale",
                 "damageTiltStrength", "glintSpeed", "glintStrength",
                 "panoramaSpeed", "entityDistanceScaling" -> String.format("%.0f%%", value * 100);
            case "chatDelay" -> value <= 0.0 ? "options.chat.delay_none" : String.format("%.1f", value);
            case "notificationDisplayTime" -> String.format("%.1f", value);
            case "chatScale", "chatWidth", "chatHeightFocused", "chatHeightUnfocused" -> String.format("%.0f%%", value * 100);
            case "mouseWheelSensitivity" -> String.format("%.2f", value);
            default -> String.format("%.2f", value);
        };
    }

    public static int getOptionIndex(String key, Options opts) { return 0; }

    // ========== CycleButton 绑定 ==========

    private static String getCurrentOptionKey(String key, Options opts) {
        Minecraft mc = Minecraft.getInstance();
        return switch (key) {
            case "graphicsMode" -> String.valueOf(opts.graphicsMode().get().ordinal());
            case "renderClouds", "cloudStatus" -> String.valueOf(opts.cloudStatus().get().ordinal());
            case "prioritizeChunkUpdates" -> String.valueOf(opts.prioritizeChunkUpdates().get().ordinal());
            case "ao", "ambientOcclusion" -> String.valueOf(opts.ambientOcclusion().get()).toLowerCase();
            case "bobView", "viewBobbing" -> String.valueOf(opts.bobView().get()).toLowerCase();
            case "entityShadows" -> String.valueOf(opts.entityShadows().get()).toLowerCase();
            case "attackIndicator" -> String.valueOf(opts.attackIndicator().get().ordinal());
            case "enableVsync", "vsync" -> String.valueOf(opts.enableVsync().get()).toLowerCase();
            case "particles", "particle" -> String.valueOf(opts.particles().get().getId()).toLowerCase();
            case "biomeBlendRadius" -> String.valueOf(opts.biomeBlendRadius().get()).toLowerCase();
            case "showAutosaveIndicator", "autosaveIndicator" -> String.valueOf(opts.showAutosaveIndicator().get()).toLowerCase();
            case "showSubtitles" -> String.valueOf(opts.showSubtitles().get()).toLowerCase();
            case "directionalAudio" -> String.valueOf(opts.directionalAudio().get()).toLowerCase();
            case "soundDevice" -> opts.soundDevice().get() != null ? opts.soundDevice().get() : "";
            case "guiScale" -> String.valueOf(opts.guiScale().get()).toLowerCase();
            case "fullscreen" -> String.valueOf(opts.fullscreen().get()).toLowerCase();
            case "difficulty" -> mc.level != null
                    ? mc.level.getDifficulty().getKey().replace(" ", "_").toLowerCase()
                    : "normal";
            case "narrator" -> String.valueOf(opts.narrator().get().ordinal());
            case "highContrast" -> String.valueOf(opts.highContrast().get()).toLowerCase();
            case "autoJump" -> String.valueOf(opts.autoJump().get()).toLowerCase();
            case "toggleCrouch" -> String.valueOf(opts.toggleCrouch().get()).toLowerCase();
            case "toggleSprint" -> String.valueOf(opts.toggleSprint().get()).toLowerCase();
            case "operatorItemsTab" -> String.valueOf(opts.operatorItemsTab().get()).toLowerCase();
            case "backgroundForChatOnly" -> String.valueOf(opts.backgroundForChatOnly().get()).toLowerCase();
            case "hideLightningFlash" -> String.valueOf(opts.hideLightningFlash().get()).toLowerCase();
            case "darkMojangStudiosBackground" -> String.valueOf(opts.darkMojangStudiosBackground().get()).toLowerCase();
            case "hideSplashTexts" -> String.valueOf(opts.hideSplashTexts().get()).toLowerCase();
            case "narratorHotkey" -> String.valueOf(opts.narratorHotkey().get()).toLowerCase();
            case "mainHand" -> String.valueOf(opts.mainHand().get().ordinal());
            case "modelPartCape" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.CAPE)).toLowerCase();
            case "modelPartJacket" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.JACKET)).toLowerCase();
            case "modelPartLeftSleeve" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE)).toLowerCase();
            case "modelPartRightSleeve" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE)).toLowerCase();
            case "modelPartLeftPantsLeg" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.LEFT_PANTS_LEG)).toLowerCase();
            case "modelPartRightPantsLeg" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_PANTS_LEG)).toLowerCase();
            case "modelPartHat" -> String.valueOf(opts.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.HAT)).toLowerCase();
            case "chatVisibility" -> String.valueOf(opts.chatVisibility().get().ordinal());
            case "chatColors" -> String.valueOf(opts.chatColors().get()).toLowerCase();
            case "chatLinks" -> String.valueOf(opts.chatLinks().get()).toLowerCase();
            case "chatLinksPrompt" -> String.valueOf(opts.chatLinksPrompt().get()).toLowerCase();
            case "autoSuggestions" -> String.valueOf(opts.autoSuggestions().get()).toLowerCase();
            case "hideMatchedNames" -> String.valueOf(opts.hideMatchedNames().get()).toLowerCase();
            case "reducedDebugInfo" -> String.valueOf(opts.reducedDebugInfo().get()).toLowerCase();
            case "onlyShowSecureChat" -> String.valueOf(opts.onlyShowSecureChat().get()).toLowerCase();
            case "invertYMouse" -> String.valueOf(opts.invertYMouse().get()).toLowerCase();
            case "discreteMouseScroll" -> String.valueOf(opts.discreteMouseScroll().get()).toLowerCase();
            case "touchscreen" -> String.valueOf(opts.touchscreen().get()).toLowerCase();
            case "rawMouseInput" -> String.valueOf(opts.rawMouseInput().get()).toLowerCase();
            case "forceUnicodeFont" -> String.valueOf(opts.forceUnicodeFont().get()).toLowerCase();
            case "japaneseGlyphVariants" -> String.valueOf(opts.japaneseGlyphVariants().get()).toLowerCase();
            case "realmsNotifications" -> String.valueOf(opts.realmsNotifications().get()).toLowerCase();
            case "allowServerListing" -> String.valueOf(opts.allowServerListing().get()).toLowerCase();
            case "telemetryOptInExtra" -> String.valueOf(opts.telemetryOptInExtra().get()).toLowerCase();
            case "hideServerAddress" -> String.valueOf(opts.hideServerAddress).toLowerCase();
            case "advancedItemTooltips" -> String.valueOf(opts.advancedItemTooltips).toLowerCase();
            case "pauseOnLostFocus" -> String.valueOf(opts.pauseOnLostFocus).toLowerCase();
            default -> "";
        };
    }

    public static int getCycleOptionIndex(String key, List<CycleValue> values, Options opts) {
        String currentKey = getCurrentOptionKey(key, opts);
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).key().equalsIgnoreCase(currentKey)) return i;
        }
        return 0;
    }

    public static void setCycleOptionValue(String key, String valueKey, Options opts) {
        Minecraft mc = Minecraft.getInstance();
        switch (key) {
            case "graphicsMode" -> {
                var statuses = net.minecraft.client.GraphicsStatus.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < statuses.length) {
                    opts.graphicsMode().set(statuses[idx]);
                    mc.levelRenderer.allChanged();
                }
            }
            case "renderClouds", "cloudStatus" -> {
                var statuses = net.minecraft.client.CloudStatus.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < statuses.length) {
                    opts.cloudStatus().set(statuses[idx]);
                }
            }
            case "prioritizeChunkUpdates" -> {
                var values = net.minecraft.client.PrioritizeChunkUpdates.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < values.length) {
                    opts.prioritizeChunkUpdates().set(values[idx]);
                }
            }
            case "ao", "ambientOcclusion" -> {
                opts.ambientOcclusion().set(Boolean.parseBoolean(valueKey));
                mc.levelRenderer.allChanged();
            }
            case "bobView", "viewBobbing" -> opts.bobView().set(Boolean.parseBoolean(valueKey));
            case "entityShadows" -> opts.entityShadows().set(Boolean.parseBoolean(valueKey));
            case "attackIndicator" -> {
                var statuses = net.minecraft.client.AttackIndicatorStatus.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < statuses.length) {
                    opts.attackIndicator().set(statuses[idx]);
                }
            }
            case "enableVsync", "vsync" -> opts.enableVsync().set(Boolean.parseBoolean(valueKey));
            case "particles", "particle" -> {
                for (net.minecraft.client.ParticleStatus value : net.minecraft.client.ParticleStatus.values()) {
                    if (String.valueOf(value.getId()).equalsIgnoreCase(valueKey)) {
                        opts.particles().set(value);
                        break;
                    }
                }
            }
            case "biomeBlendRadius" -> opts.biomeBlendRadius().set(Integer.parseInt(valueKey));
            case "showAutosaveIndicator", "autosaveIndicator" -> opts.showAutosaveIndicator().set(Boolean.parseBoolean(valueKey));
            case "showSubtitles" -> opts.showSubtitles().set(Boolean.parseBoolean(valueKey));
            case "directionalAudio" -> opts.directionalAudio().set(Boolean.parseBoolean(valueKey));
            case "soundDevice" -> opts.soundDevice().set(valueKey);
            case "guiScale" -> opts.guiScale().set(Integer.parseInt(valueKey));
            case "fullscreen" -> {
                mc.getWindow().toggleFullScreen();
                opts.fullscreen().set(mc.getWindow().isFullscreen());
            }
            case "difficulty" -> {
                if (mc.getConnection() != null) {
                    Difficulty diff = Difficulty.byName(valueKey);
                    mc.getConnection().send(
                            new net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket(diff));
                }
            }
            case "narrator" -> {
                var statuses = net.minecraft.client.NarratorStatus.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < statuses.length) {
                    opts.narrator().set(statuses[idx]);
                }
            }
            case "highContrast" -> opts.highContrast().set(Boolean.parseBoolean(valueKey));
            case "autoJump" -> opts.autoJump().set(Boolean.parseBoolean(valueKey));
            case "toggleCrouch" -> opts.toggleCrouch().set(Boolean.parseBoolean(valueKey));
            case "toggleSprint" -> opts.toggleSprint().set(Boolean.parseBoolean(valueKey));
            case "operatorItemsTab" -> opts.operatorItemsTab().set(Boolean.parseBoolean(valueKey));
            case "backgroundForChatOnly" -> opts.backgroundForChatOnly().set(Boolean.parseBoolean(valueKey));
            case "hideLightningFlash" -> opts.hideLightningFlash().set(Boolean.parseBoolean(valueKey));
            case "darkMojangStudiosBackground" -> opts.darkMojangStudiosBackground().set(Boolean.parseBoolean(valueKey));
            case "hideSplashTexts" -> opts.hideSplashTexts().set(Boolean.parseBoolean(valueKey));
            case "narratorHotkey" -> opts.narratorHotkey().set(Boolean.parseBoolean(valueKey));
            case "mainHand" -> {
                var values = net.minecraft.world.entity.HumanoidArm.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < values.length) {
                    opts.mainHand().set(values[idx]);
                }
            }
            case "modelPartCape" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.CAPE, Boolean.parseBoolean(valueKey));
            case "modelPartJacket" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.JACKET, Boolean.parseBoolean(valueKey));
            case "modelPartLeftSleeve" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE, Boolean.parseBoolean(valueKey));
            case "modelPartRightSleeve" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE, Boolean.parseBoolean(valueKey));
            case "modelPartLeftPantsLeg" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.LEFT_PANTS_LEG, Boolean.parseBoolean(valueKey));
            case "modelPartRightPantsLeg" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_PANTS_LEG, Boolean.parseBoolean(valueKey));
            case "modelPartHat" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.HAT, Boolean.parseBoolean(valueKey));
            case "chatVisibility" -> {
                var statuses = net.minecraft.world.entity.player.ChatVisiblity.values();
                int idx = Integer.parseInt(valueKey);
                if (idx >= 0 && idx < statuses.length) {
                    opts.chatVisibility().set(statuses[idx]);
                }
            }
            case "chatColors" -> opts.chatColors().set(Boolean.parseBoolean(valueKey));
            case "chatLinks" -> opts.chatLinks().set(Boolean.parseBoolean(valueKey));
            case "chatLinksPrompt" -> opts.chatLinksPrompt().set(Boolean.parseBoolean(valueKey));
            case "autoSuggestions" -> opts.autoSuggestions().set(Boolean.parseBoolean(valueKey));
            case "hideMatchedNames" -> opts.hideMatchedNames().set(Boolean.parseBoolean(valueKey));
            case "reducedDebugInfo" -> opts.reducedDebugInfo().set(Boolean.parseBoolean(valueKey));
            case "onlyShowSecureChat" -> opts.onlyShowSecureChat().set(Boolean.parseBoolean(valueKey));
            case "invertYMouse" -> opts.invertYMouse().set(Boolean.parseBoolean(valueKey));
            case "discreteMouseScroll" -> opts.discreteMouseScroll().set(Boolean.parseBoolean(valueKey));
            case "touchscreen" -> opts.touchscreen().set(Boolean.parseBoolean(valueKey));
            case "rawMouseInput" -> opts.rawMouseInput().set(Boolean.parseBoolean(valueKey));
            case "forceUnicodeFont" -> opts.forceUnicodeFont().set(Boolean.parseBoolean(valueKey));
            case "japaneseGlyphVariants" -> opts.japaneseGlyphVariants().set(Boolean.parseBoolean(valueKey));
            case "realmsNotifications" -> opts.realmsNotifications().set(Boolean.parseBoolean(valueKey));
            case "allowServerListing" -> opts.allowServerListing().set(Boolean.parseBoolean(valueKey));
            case "telemetryOptInExtra" -> opts.telemetryOptInExtra().set(Boolean.parseBoolean(valueKey));
            case "hideServerAddress" -> opts.hideServerAddress = Boolean.parseBoolean(valueKey);
            case "advancedItemTooltips" -> opts.advancedItemTooltips = Boolean.parseBoolean(valueKey);
            case "pauseOnLostFocus" -> opts.pauseOnLostFocus = Boolean.parseBoolean(valueKey);
        }
    }

    public static void resetOptionToDefault(String key, Options opts) {
        Minecraft mc = Minecraft.getInstance();
        switch (key) {
            case "graphicsMode" -> {
                opts.graphicsMode().set(net.minecraft.client.GraphicsStatus.FANCY);
                mc.levelRenderer.allChanged();
            }
            case "renderClouds", "cloudStatus" -> {
                opts.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
            }
            case "prioritizeChunkUpdates" -> {
                opts.prioritizeChunkUpdates().set(net.minecraft.client.PrioritizeChunkUpdates.NONE);
            }
            case "ao", "ambientOcclusion" -> {
                opts.ambientOcclusion().set(true);
                mc.levelRenderer.allChanged();
            }
            case "bobView", "viewBobbing" -> opts.bobView().set(true);
            case "entityShadows" -> opts.entityShadows().set(true);
            case "attackIndicator" -> {
                opts.attackIndicator().set(net.minecraft.client.AttackIndicatorStatus.CROSSHAIR);
            }
            case "enableVsync", "vsync" -> opts.enableVsync().set(true);
            case "particles", "particle" -> {
                opts.particles().set(net.minecraft.client.ParticleStatus.ALL);
            }
            case "biomeBlendRadius" -> opts.biomeBlendRadius().set(2);
            case "showAutosaveIndicator", "autosaveIndicator" -> opts.showAutosaveIndicator().set(true);
            case "showSubtitles" -> opts.showSubtitles().set(false);
            case "directionalAudio" -> opts.directionalAudio().set(false);
            case "guiScale" -> opts.guiScale().set(0);
            case "fullscreen" -> {
                if (mc.getWindow().isFullscreen()) {
                    mc.getWindow().toggleFullScreen();
                    opts.fullscreen().set(false);
                }
            }
            case "narrator" -> {
                opts.narrator().set(net.minecraft.client.NarratorStatus.OFF);
            }
            case "highContrast" -> opts.highContrast().set(false);
            case "autoJump" -> opts.autoJump().set(false);
            case "toggleCrouch" -> opts.toggleCrouch().set(false);
            case "toggleSprint" -> opts.toggleSprint().set(false);
            case "operatorItemsTab" -> opts.operatorItemsTab().set(false);
            case "backgroundForChatOnly" -> opts.backgroundForChatOnly().set(true);
            case "hideLightningFlash" -> opts.hideLightningFlash().set(false);
            case "darkMojangStudiosBackground" -> opts.darkMojangStudiosBackground().set(false);
            case "hideSplashTexts" -> opts.hideSplashTexts().set(false);
            case "narratorHotkey" -> opts.narratorHotkey().set(true);
            case "mainHand" -> {
                opts.mainHand().set(net.minecraft.world.entity.HumanoidArm.RIGHT);
            }
            case "chatVisibility" -> {
                opts.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.FULL);
            }
            case "chatColors" -> opts.chatColors().set(true);
            case "chatLinks" -> opts.chatLinks().set(true);
            case "chatLinksPrompt" -> opts.chatLinksPrompt().set(true);
            case "autoSuggestions" -> opts.autoSuggestions().set(true);
            case "hideMatchedNames" -> opts.hideMatchedNames().set(true);
            case "reducedDebugInfo" -> opts.reducedDebugInfo().set(false);
            case "onlyShowSecureChat" -> opts.onlyShowSecureChat().set(false);
            case "soundDevice" -> opts.soundDevice().set("");
            case "difficulty" -> { }
            case "modelPartCape" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.CAPE, true);
            case "modelPartJacket" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.JACKET, true);
            case "modelPartLeftSleeve" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE, true);
            case "modelPartRightSleeve" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE, true);
            case "modelPartLeftPantsLeg" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.LEFT_PANTS_LEG, true);
            case "modelPartRightPantsLeg" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_PANTS_LEG, true);
            case "modelPartHat" -> opts.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.HAT, true);
            case "invertYMouse" -> opts.invertYMouse().set(false);
            case "discreteMouseScroll" -> opts.discreteMouseScroll().set(false);
            case "touchscreen" -> opts.touchscreen().set(false);
            case "rawMouseInput" -> opts.rawMouseInput().set(true);
            case "forceUnicodeFont" -> opts.forceUnicodeFont().set(false);
            case "japaneseGlyphVariants" -> opts.japaneseGlyphVariants().set(false);
            case "realmsNotifications" -> opts.realmsNotifications().set(true);
            case "allowServerListing" -> opts.allowServerListing().set(true);
            case "telemetryOptInExtra" -> opts.telemetryOptInExtra().set(false);
            case "hideServerAddress" -> opts.hideServerAddress = false;
            case "advancedItemTooltips" -> opts.advancedItemTooltips = false;
            case "pauseOnLostFocus" -> opts.pauseOnLostFocus = true;
            case "mouseWheelSensitivity" -> opts.mouseWheelSensitivity().set(1.0);
        }
    }
}