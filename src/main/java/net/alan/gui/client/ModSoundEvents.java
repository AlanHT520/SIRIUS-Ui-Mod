package net.alan.gui.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.alan.gui.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModSoundEvents {
    private static final String SOUNDS_JSON_PATH = "/assets/minecraft/alanht/sounds.json";

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MOD_ID);

    private static final Map<String, RegistryObject<SoundEvent>> REGISTERED_SOUNDS = new LinkedHashMap<>();

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
                ResourceLocation id = new ResourceLocation(Main.MOD_ID, soundName);
                RegistryObject<SoundEvent> entry = SOUND_EVENTS.register(soundName,
                        () -> SoundEvent.createVariableRangeEvent(id));
                REGISTERED_SOUNDS.put(soundName, entry);
            }
        } catch (Exception ignored) {
        }
    }

    public static RegistryObject<SoundEvent> getSound(String name) {
        return REGISTERED_SOUNDS.get(name);
    }
}