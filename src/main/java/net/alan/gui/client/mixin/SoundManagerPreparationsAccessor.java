package net.alan.gui.client.mixin;

import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public interface SoundManagerPreparationsAccessor {

    @Invoker("handleRegistration")
    void sirius$invokeHandleRegistration(ResourceLocation location, SoundEventRegistration registration);
}