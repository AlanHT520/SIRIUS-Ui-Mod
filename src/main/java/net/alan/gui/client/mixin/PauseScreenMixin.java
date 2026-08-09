package net.alan.gui.client.mixin;

import net.alan.gui.Config;
import net.alan.gui.Main;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.screen.ScreenLayout;
import net.alan.gui.registry.JsonScreenRegistry;
import net.alan.gui.render.screen.JsonScreenRenderer;
import net.alan.gui.util.JsonLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    @Unique
    private JsonScreenRenderer alan$uiRenderer;

    protected PauseScreenMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alan$onInit(CallbackInfo ci) {
        PauseScreen screen = (PauseScreen) (Object) this;
        if (!Config.ENABLE_CUSTOM_UI.get()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ResourceManager resourceManager = client.getResourceManager();

        ResourceLocation layoutId = JsonScreenRegistry.getLayoutId("pauseScreen")
                .orElse(new ResourceLocation(Main.MOD_ID, "screens/pause_screen.json"));
        ScreenLayout layout = JsonLoader.loadScreenLayout(resourceManager, layoutId);

        if (layout != null) {
            String screenId = ScreenVariableRegistry.extractScreenId(layoutId);
            this.alan$uiRenderer = new JsonScreenRenderer(client, this, layout, screenId);
        }
    }

    @Unique
    private boolean alan$isActive() {
        return alan$uiRenderer != null && Config.ENABLE_CUSTOM_UI.get();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (alan$isActive()) {
            alan$uiRenderer.render(graphics, mouseX, mouseY, delta);
            ci.cancel();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (alan$isActive()) {
            return alan$uiRenderer.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (alan$isActive()) {
            return alan$uiRenderer.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (alan$isActive()) {
            return alan$uiRenderer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (alan$isActive()) {
            return alan$uiRenderer.mouseScrolled(mouseX, mouseY, 0, scrollDelta);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (alan$isActive()) {
            return alan$uiRenderer.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (alan$isActive()) {
            return alan$uiRenderer.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}