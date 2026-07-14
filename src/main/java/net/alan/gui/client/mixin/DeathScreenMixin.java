package net.alan.gui.client.mixin;

import net.alan.gui.Config;
import net.alan.gui.Main;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.config.ScreenLayout;
import net.alan.gui.render.JsonScreenRenderer;
import net.alan.gui.util.JsonLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    @Unique
    private JsonScreenRenderer alan$uiRenderer;

    protected DeathScreenMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alan$onInit(CallbackInfo ci) {
        if (!Config.ENABLE_CUSTOM_UI.get()) return;
        Minecraft client = Minecraft.getInstance();
        ResourceManager resourceManager = client.getResourceManager();

        ScreenLayout layout = JsonLoader.loadScreenLayout(resourceManager,
                ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "screens/death_screen.json"));

        if (layout != null) {
            this.alan$uiRenderer = new JsonScreenRenderer(client, this, layout, "death_screen");
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

    /**
     * @author AlanHT520
     * @reason use the method for json button
     */
    @Overwrite
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (alan$uiRenderer != null) {
            return alan$uiRenderer.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}