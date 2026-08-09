package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.entity.FakeLevel;
import net.alan.gui.entity.FakePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

public class EntityDisplayWidget extends BaseWidget {

    private final String entityType;
    private final float scale;
    private final boolean lookAtMouse;
    private final String animState;
    private final float walkSpeed;
    private final float walkAmplitude;
    private final boolean attackEnabled;
    private final float offsetX;
    private final float offsetY;
    private final float lookSensitivityX;
    private final float lookSensitivityY;

    private LivingEntity cachedEntity;

    public EntityDisplayWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                                String entityType, float scale, boolean lookAtMouse,
                                String animState, float walkSpeed, float walkAmplitude, boolean attackEnabled,
                                float offsetX, float offsetY, float lookSensitivityX, float lookSensitivityY) {
        super(id, layout, variables, member);
        this.entityType = entityType;
        this.scale = scale;
        this.lookAtMouse = lookAtMouse;
        this.animState = animState;
        this.walkSpeed = walkSpeed;
        this.walkAmplitude = walkAmplitude;
        this.attackEnabled = attackEnabled;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.lookSensitivityX = lookSensitivityX;
        this.lookSensitivityY = lookSensitivityY;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                        RenderContext context, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0 || !layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        LivingEntity entity = getEntity();
        if (entity == null) return;

        updateEntityAnimation(entity);

        float centerX = screenX + dim.w / 2.0F + offsetX;
        float centerY = screenY + dim.h / 2.0F + offsetY;

        float angleX, angleY;
        if (lookAtMouse) {
            angleX = (centerX - mouseX) / (float) dim.w * lookSensitivityX;
            angleY = (centerY - mouseY) / (float) dim.h * lookSensitivityY;
        } else {
            float time = (System.currentTimeMillis() % 10000) / 10000.0F;
            angleX = (float) Math.sin(time * Math.PI * 2) * 0.15F;
            angleY = 0.0f;
        }

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf().rotateX(angleY * 20.0F * (float) (Math.PI / 180.0));
        pose.mul(cameraOrientation);

        float savedBodyRot = entity.yBodyRot;
        float savedBodyRotO = entity.yBodyRotO;
        float savedYRot = entity.getYRot();
        float savedXRot = entity.getXRot();
        float savedXRotO = entity.xRotO;
        float savedHeadRotO = entity.yHeadRotO;
        float savedHeadRot = entity.yHeadRot;

        entity.yBodyRot = 180.0F + angleX * 20.0F;
        entity.yBodyRotO = entity.yBodyRot;
        entity.setYRot(180.0F + angleX * 40.0F);
        entity.xRotO = -angleY * 20.0F;
        entity.setXRot(-angleY * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        float entityScale = entity.getScale();
        float renderScale = Math.round(scale) / entityScale;
        Vector3f translate = new Vector3f(0.0F, entity.getBbHeight() / 2.0F, 0.0F);

        InventoryScreen.renderEntityInInventory(
            graphics, centerX, centerY, renderScale, translate, pose, cameraOrientation, entity
        );

        entity.yBodyRot = savedBodyRot;
        entity.yBodyRotO = savedBodyRotO;
        entity.setYRot(savedYRot);
        entity.xRotO = savedXRotO;
        entity.setXRot(savedXRot);
        entity.yHeadRotO = savedHeadRotO;
        entity.yHeadRot = savedHeadRot;
    }

    private void updateEntityAnimation(LivingEntity entity) {
        if (entity instanceof FakePlayer fakePlayer) {
            fakePlayer.setAnimationConfig(animState, walkSpeed, walkAmplitude, attackEnabled);
            fakePlayer.updateAnimation();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                RenderContext context, int x, int y, int width, int height) {
        if (!attackEnabled) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        if (button == 0 && mouseX >= screenX && mouseX <= screenX + dim.w
                && mouseY >= screenY && mouseY <= screenY + dim.h) {
            LivingEntity entity = getEntity();
            if (entity instanceof FakePlayer fakePlayer) {
                fakePlayer.triggerSwing();
            }
            return true;
        }
        return false;
    }

    private LivingEntity getEntity() {
        if (cachedEntity != null) return cachedEntity;

        Minecraft mc = Minecraft.getInstance();
        if ("player".equals(entityType)) {
            if (mc.player != null) {
                cachedEntity = mc.player;
            } else {
                FakeLevel fakeLevel = FakeLevel.getInstance();
                if (fakeLevel != null && mc.getGameProfile() != null) {
                    cachedEntity = fakeLevel.createPlayer();
                }
            }
        }
        return cachedEntity;
    }
}