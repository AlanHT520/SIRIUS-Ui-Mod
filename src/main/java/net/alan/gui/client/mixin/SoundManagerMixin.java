package net.alan.gui.client.mixin;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.Reader;
import java.util.Map;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Unique
    private static final Gson sirius$GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
            .registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer())
            .create();

    @Unique
    private static final TypeToken<Map<String, SoundEventRegistration>> sirius$SOUND_EVENT_REGISTRATION_TYPE =
            new TypeToken<Map<String, SoundEventRegistration>>() {};

    @Unique
    private static final ResourceLocation sirius$CUSTOM_SOUNDS_JSON =
            new ResourceLocation("minecraft", "alanht/sounds.json");

    @Unique
    private static final String sirius$SOUND_NAMESPACE = "sirius_ui";

    @Inject(method = "prepare", at = @At("TAIL"))
    private void sirius$injectCustomSounds(ResourceManager resourceManager, ProfilerFiller profiler,
                                            CallbackInfoReturnable<Object> cir) {
        Object preparations = cir.getReturnValue();
        SoundManagerPreparationsAccessor accessor = (SoundManagerPreparationsAccessor) preparations;

        try {
            for (Resource resource : resourceManager.getResourceStack(sirius$CUSTOM_SOUNDS_JSON)) {
                try (Reader reader = resource.openAsReader()) {
                    Map<String, SoundEventRegistration> map =
                            sirius$GSON.fromJson(reader, sirius$SOUND_EVENT_REGISTRATION_TYPE.getType());
                    for (Map.Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                        ResourceLocation soundLocation =
                                new ResourceLocation(sirius$SOUND_NAMESPACE, entry.getKey());
                        accessor.sirius$invokeHandleRegistration(soundLocation, entry.getValue());
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略自定义音效加载错误
        }
    }
}