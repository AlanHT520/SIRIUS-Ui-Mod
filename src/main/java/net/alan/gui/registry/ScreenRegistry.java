package net.alan.gui.registry;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.alan.gui.elements.JsonScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.*;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ScreenRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenRegistry.class);
    private static final Map<String, Function<Screen, Screen>> REGISTRY = new HashMap<>();

    static {
        register("world_select", parent -> new SelectWorldScreen(parent));
        register("multiplayer", parent -> new JoinMultiplayerScreen(parent));
        register("realms", parent -> new RealmsMainScreen(parent));
        register("options", parent -> new OptionsScreen(parent, Minecraft.getInstance().options));
        register("video_settings", parent -> new VideoSettingsScreen(parent, Minecraft.getInstance(), Minecraft.getInstance().options));
        register("sound_settings", parent -> new SoundOptionsScreen(parent, Minecraft.getInstance().options));
        register("keybinds", parent -> new KeyBindsScreen(parent, Minecraft.getInstance().options));
        register("chat_settings", parent -> new ChatOptionsScreen(parent, Minecraft.getInstance().options));
        register("accessibility", parent -> new AccessibilityOptionsScreen(parent, Minecraft.getInstance().options));
        register("language", parent -> new LanguageSelectScreen(
                parent,
                Minecraft.getInstance().options,
                Minecraft.getInstance().getLanguageManager()
        ));
        register("controls", parent -> new ControlsScreen(parent, Minecraft.getInstance().options));
        register("skin_customization", parent -> new SkinCustomizationScreen(parent, Minecraft.getInstance().options));
        register("telemetry", parent -> new TelemetryInfoScreen(parent, Minecraft.getInstance().options));
        register("credits", parent -> new CreditsAndAttributionScreen(parent));
        register("player_social", parent -> new SocialInteractionsScreen());
        register("advancements", parent -> new AdvancementsScreen(Minecraft.getInstance().player.connection.getAdvancements(), parent));
        register("stats", parent -> new StatsScreen(parent, Minecraft.getInstance().player.getStats()));
        register("share_to_lan", parent -> new ShareToLanScreen(parent));

        register("mods_list", parent -> {
            try {
                Class<?> modListScreenClass = Class.forName("net.neoforged.neoforge.client.gui.ModListScreen");
                return (Screen) modListScreenClass.getConstructor(Screen.class).newInstance(parent);
            } catch (Exception e) {
                LOGGER.error("Failed to open NeoForge mod list screen", e);
                return null;
            }
        });
    }

    public static void register(String id, Function<Screen, Screen> factory) {
        if (REGISTRY.containsKey(id)) LOGGER.warn("Overriding existing screen registration for id: {}", id);
        REGISTRY.put(id, factory);
    }

    public static Screen openScreen(String id, Screen parent) {
        if (!JsonScreenRegistry.isLoaded()) {
            try {
                JsonScreenRegistry.load(Minecraft.getInstance().getResourceManager());
            } catch (Exception e) {
                LOGGER.error("Failed to lazy load JSON screen registry", e);
            }
        }
        Optional<ResourceLocation> jsonLayout = JsonScreenRegistry.getLayoutId(id);
        if (jsonLayout.isPresent()) {
            return new JsonScreen(parent, jsonLayout.get());
        }
        Function<Screen, Screen> factory = REGISTRY.get(id);
        if (factory == null) {
            LOGGER.error("No screen registered for id: {}", id);
            return null;
        }
        return factory.apply(parent);
    }
}