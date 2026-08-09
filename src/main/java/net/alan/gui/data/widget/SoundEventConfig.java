package net.alan.gui.data.widget;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventConfig {
    private String id;
    private float volume;
    private float pitch;
    private int throttleTicks = 0;
    private Holder<SoundEvent> soundEvent;

    private static final Map<String, Holder<SoundEvent>> SOUND_CACHE = new ConcurrentHashMap<>();

    public SoundEventConfig() {
        this.volume = 1.0f;
        this.pitch = 1.0f;
    }

    public SoundEventConfig(String id, float volume, float pitch) {
        this.id = id;
        this.volume = volume;
        this.pitch = pitch;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.soundEvent = null;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
    }

    public int getThrottleTicks() {
        return throttleTicks;
    }

    public void setThrottleTicks(int throttleTicks) {
        this.throttleTicks = Math.max(0, throttleTicks);
    }

    public Holder<SoundEvent> getSoundEvent() {
        if (soundEvent == null && id != null) {
            soundEvent = resolveSoundEvent(id);
        }
        return soundEvent;
    }

    private static Holder<SoundEvent> resolveSoundEvent(String id) {
        Holder<SoundEvent> cached = SOUND_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        try {
            if (id.contains(":")) {
                ResourceLocation location = new ResourceLocation(id);
                if (BuiltInRegistries.SOUND_EVENT.containsKey(location)) {
                    Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.getHolder(ResourceKey.create(Registries.SOUND_EVENT, location)).orElse(null);
                    if (holder != null) {
                        SOUND_CACHE.put(id, holder);
                    }
                    return holder;
                }
            } else {
                for (var entry : BuiltInRegistries.SOUND_EVENT.entrySet()) {
                    if (entry.getKey().location().getPath().equals(id)) {
                        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.getHolder(entry.getKey()).orElse(null);
                        if (holder != null) {
                            SOUND_CACHE.put(id, holder);
                        }
                        return holder;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }

    public boolean isValid() {
        return id != null && !id.isEmpty() && getSoundEvent() != null;
    }

    public static Holder<SoundEvent> getDefaultClickSound() {
        return SoundEvents.UI_BUTTON_CLICK;
    }
}