package net.alan.gui.entity;

import com.mojang.authlib.GameProfile;
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
    private boolean attackEnabled = true;

    public FakePlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    public void setAnimationConfig(String state, float walkSpeed, boolean attackEnabled) {
        this.currentState = state;
        this.walkSpeed = walkSpeed;
        this.attackEnabled = attackEnabled;
    }

    public void triggerSwing() {
        if (!attackEnabled) return;
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
        }
    }

    public void updateAnimation() {
        this.tickCount++;

        if ("walk".equals(currentState)) {
            this.walkAnimation.update(walkSpeed, 0.4F);
        }

        if (this.swinging) {
            this.swingTime++;
            if (this.swingTime >= this.getCurrentSwingDuration()) {
                this.swingTime = 0;
                this.swinging = false;
            }
        }
        this.attackAnim = (float) this.swingTime / (float) this.getCurrentSwingDuration();
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