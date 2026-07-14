package net.alan.gui.elements;

import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.config.ScreenLayout;
import net.alan.gui.render.JsonScreenRenderer;
import net.alan.gui.util.JsonLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class JsonScreen extends Screen {
    private final Screen lastScreen;
    private final ResourceLocation layoutId;
    private JsonScreenRenderer renderer;

    public JsonScreen(Screen lastScreen, ResourceLocation layoutId) {
        super(Component.empty());
        this.lastScreen = lastScreen;
        this.layoutId = layoutId;
    }

    @Override
    protected void init() {
        super.init();
        if (this.renderer != null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ScreenLayout layout = JsonLoader.loadScreenLayout(minecraft.getResourceManager(), layoutId);
        if (layout != null) {
            String screenId = ScreenVariableRegistry.extractScreenId(layoutId);
            this.renderer = new JsonScreenRenderer(minecraft, this, layout, screenId);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (renderer != null) {
            renderer.render(graphics, mouseX, mouseY, delta);
        } else {
            super.render(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (renderer != null && renderer.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (renderer != null && renderer.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (renderer != null && renderer.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (renderer != null && renderer.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renderer != null && renderer.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (renderer != null && renderer.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.options.save();
        Screen root = findRootScreen(lastScreen);
        if (root != null) {
            minecraft.setScreen(root);
        } else {
            super.onClose();
        }
    }

    private static Screen findRootScreen(Screen screen) {
        if (screen == null) return null;
        java.util.Set<Screen> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Screen current = screen;
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            if (current instanceof JsonScreen jsonScreen) {
                try {
                    var field = JsonScreen.class.getDeclaredField("lastScreen");
                    field.setAccessible(true);
                    Screen last = (Screen) field.get(jsonScreen);
                    if (last != null) {
                        current = last;
                        continue;
                    }
                } catch (Exception e) {
                    break;
                }
                break;
            }
            Screen parent = getParentFromNativeScreen(current);
            if (parent != null) {
                current = parent;
                continue;
            }
            break;
        }
        return current;
    }

    private static Screen getParentFromNativeScreen(Screen screen) {
        for (String fieldName : new String[]{"parent", "lastScreen"}) {
            try {
                var field = screen.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(screen);
                if (value instanceof Screen parent) {
                    return parent;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                break;
            }
        }
        return null;
    }
}