package net.alan.gui.client.mixin;

import net.alan.gui.Config;
import net.alan.gui.Main;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.config.ScreenLayout;
import net.alan.gui.registry.JsonScreenRegistry;
import net.alan.gui.render.JsonScreenRenderer;
import net.alan.gui.util.JsonLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private JsonScreenRenderer sirius$uiRenderer;

    protected TitleScreenMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sirius$onInit(CallbackInfo ci) {
        if (!Config.ENABLE_CUSTOM_UI.get()) return;
        Minecraft client = Minecraft.getInstance();
        ResourceManager rm = client.getResourceManager();

        ResourceLocation layoutId = JsonScreenRegistry.getLayoutId("titleScreen")
                .orElse(ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "screens/title_screen.json"));
        ScreenLayout layout = JsonLoader.loadScreenLayout(rm, layoutId);

        if (layout != null) {
            String screenId = ScreenVariableRegistry.extractScreenId(layoutId);
            this.sirius$uiRenderer = new JsonScreenRenderer(client, (TitleScreen) (Object) this, layout, screenId);
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
        } catch (Exception ignored) {}
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sirius$onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (sirius$uiRenderer != null) {
            sirius$uiRenderer.render(graphics, mouseX, mouseY, delta);
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sirius$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (sirius$uiRenderer != null) {
            boolean handled = sirius$uiRenderer.mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(handled);
            cir.cancel();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sirius$uiRenderer != null) {
            return sirius$uiRenderer.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (sirius$uiRenderer != null) {
            return sirius$uiRenderer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (sirius$uiRenderer != null) {
            return sirius$uiRenderer.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (sirius$uiRenderer != null) {
            return sirius$uiRenderer.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (sirius$uiRenderer != null) {
            return sirius$uiRenderer.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}