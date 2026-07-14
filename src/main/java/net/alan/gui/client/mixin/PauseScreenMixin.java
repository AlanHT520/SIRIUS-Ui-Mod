package net.alan.gui.client.mixin;

import net.alan.gui.Config;
import net.alan.gui.Main;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.config.ScreenLayout;
import net.alan.gui.render.JsonScreenRenderer;
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

import java.lang.reflect.Field;
import java.util.List;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    @Unique
    private JsonScreenRenderer alan$uiRenderer;

    protected PauseScreenMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alan$onInit(CallbackInfo ci) {
        if (!Config.ENABLE_CUSTOM_UI.get()) return;
        PauseScreen screen = (PauseScreen) (Object) this;
        if (!screen.showsPauseMenu()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ResourceManager resourceManager = client.getResourceManager();

        ScreenLayout layout = JsonLoader.loadScreenLayout(resourceManager,
                ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "screens/pause_screen.json"));

        if (layout != null) {
            this.alan$uiRenderer = new JsonScreenRenderer(client, this, layout, "pause_screen");
            clearOriginalWidgets();
        }
    }

    @Unique
    private void clearOriginalWidgets() {
        try {
            this.children().clear();
            Field renderablesField = Screen.class.getDeclaredField("renderables");
            renderablesField.setAccessible(true);
            List<?> renderables = (List<?>) renderablesField.get(this);
            renderables.clear();
        } catch (Exception e) {
            // ignore
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (alan$uiRenderer != null) {
            alan$uiRenderer.render(graphics, mouseX, mouseY, delta);
            ci.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (alan$uiRenderer != null) {
            ci.cancel();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}