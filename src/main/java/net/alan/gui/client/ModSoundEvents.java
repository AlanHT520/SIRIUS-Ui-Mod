package net.alan.gui.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.alan.gui.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class ModSoundEvents {
    private static final String SOUNDS_JSON_PATH = "/assets/minecraft/alanht/sounds.json";

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Main.MOD_ID);

    public static void register(IEventBus modEventBus) {
        registerSoundsFromJson();
        SOUND_EVENTS.register(modEventBus);
    }

    private static void registerSoundsFromJson() {
        try (InputStream stream = ModSoundEvents.class.getResourceAsStream(SOUNDS_JSON_PATH)) {
            if (stream == null) {
                return;
            }
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (String soundName : root.keySet()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, soundName);
                SOUND_EVENTS.register(soundName, () -> SoundEvent.createFixedRangeEvent(id, 16.0F));
            }
        } catch (Exception ignored) {
        }
    }
}