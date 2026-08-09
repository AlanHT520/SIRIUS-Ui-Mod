package net.alan.gui.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class FakePlayer extends AbstractClientPlayer {

    @Nullable
    private PlayerInfo fakePlayerInfo;

    private String currentState = "idle";
    private float walkSpeed = 0.5F;
    private float walkAmplitude = 0.4F;
    private boolean attackEnabled = true;

    public FakePlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    public void setAnimationConfig(String state, float walkSpeed, float walkAmplitude, boolean attackEnabled) {
        this.currentState = state;
        this.walkSpeed = walkSpeed;
        this.walkAmplitude = walkAmplitude;
        this.attackEnabled = attackEnabled;
    }

    public void triggerSwing() {
        if (!attackEnabled) return;
        if (!this.swinging || this.swingTime >= 6 / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
        }
    }

    private float swingAccumulator = 0;

    public void updateAnimation() {
        this.tickCount++;

        float deltaTime = Minecraft.getInstance().getFrameTime();

        if ("walk".equals(currentState)) {
            this.walkAnimation.update(walkSpeed * deltaTime, walkAmplitude);
        }

        if (this.swinging) {
            swingAccumulator += deltaTime;
            while (swingAccumulator >= 1.0F) {
                swingAccumulator -= 1.0F;
                this.swingTime++;
            }
            if (this.swingTime >= 6) {
                this.swingTime = 0;
                this.swinging = false;
                swingAccumulator = 0;
            }
        }
        this.attackAnim = (float) this.swingTime / 6.0F;
    }

    @Override
    @Nullable
    protected PlayerInfo getPlayerInfo() {
        if (fakePlayerInfo == null) {
            fakePlayerInfo = new PlayerInfo(getGameProfile(), false);
        }
        return fakePlayerInfo;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public boolean onGround() {
        return true;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isInWater() {
        return false;
    }

    @Override
    public boolean isUnderWater() {
        return false;
    }

    @Override
    public boolean isPassenger() {
        return false;
    }

    @Override
    public boolean isModelPartShown(@NotNull PlayerModelPart part) {
        return true;
    }
}