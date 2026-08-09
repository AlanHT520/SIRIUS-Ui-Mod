package net.alan.gui.widget.cycle;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.CycleValue;
import net.alan.gui.data.widget.CycleButtonProps;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.TextProps;
import net.alan.gui.data.widget.TextureSet;
import net.alan.gui.render.screen.BackgroundRenderer;
import net.alan.gui.render.screen.OptionBinder;
import net.alan.gui.util.NineSliceHelper;
import net.alan.gui.widget.BaseWidget;
import net.alan.gui.widget.TextWidget;
import net.alan.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CycleButtonWidget extends BaseWidget {
    private final CycleButtonProps cycleProps;
    private final TextureSet texture;
    private final TextProps textProps;
    private final List<Widget> children;
    private final String stateKey;
    private final int backgroundColor;
    private final int highlightedBackgroundColor;
    private int currentIndex = 0;
    private boolean isHovered = false;
    private final Minecraft minecraft;

    public CycleButtonWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                             CycleButtonProps cycleProps, TextureSet texture,
                             TextProps textProps, List<Widget> children) {
        this(id, layout, variables, member, cycleProps, texture, textProps, children, null, 0xFF3A3A3C, 0xFF505052);
    }

    public CycleButtonWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                             CycleButtonProps cycleProps, TextureSet texture,
                             TextProps textProps, List<Widget> children, String stateKey) {
        this(id, layout, variables, member, cycleProps, texture, textProps, children, stateKey, 0xFF3A3A3C, 0xFF505052);
    }

    public CycleButtonWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                             CycleButtonProps cycleProps, TextureSet texture,
                             TextProps textProps, List<Widget> children, String stateKey,
                             int backgroundColor, int highlightedBackgroundColor) {
        super(id, layout, variables, member);
        this.cycleProps = cycleProps;
        this.texture = texture;
        this.textProps = textProps;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();
        this.stateKey = stateKey;
        this.backgroundColor = backgroundColor;
        this.highlightedBackgroundColor = highlightedBackgroundColor;

        if (cycleProps.values() != null && !cycleProps.values().isEmpty()) {
            if (cycleProps.optionKey() != null) {
                int idx = OptionBinder.getCycleOptionIndex(cycleProps.optionKey(), cycleProps.values(), minecraft.options);
                this.currentIndex = Mth.clamp(idx, 0, cycleProps.values().size() - 1);
            } else if (this.member.containsKey("current_index")) {
                try {
                    int idx = Integer.parseInt(this.member.get("current_index"));
                    this.currentIndex = Mth.clamp(idx, 0, cycleProps.values().size() - 1);
                } catch (NumberFormatException ignored) {
                    this.currentIndex = 0;
                }
            } else {
                this.currentIndex = 0;
            }
            this.member.put("current_value", getCurrentDisplayText());
            this.member.put("current_index", String.valueOf(currentIndex));
            this.member.put("current_key", cycleProps.values().get(currentIndex).key());
        }

        if (textProps != null && (hasTextContent(textProps))) {
            String xPos = textProps.offsetX() != null && !textProps.offsetX().equals("0")
                    ? textProps.offsetX() : "4";
            String yPos = textProps.offsetY() != null && !textProps.offsetY().equals("0")
                    ? textProps.offsetY() : "parent.height / 2 - this.height / 2";
            this.children.add(0, new TextWidget(id + "_text",
                    new LayoutProps(xPos, yPos, "auto", "auto", true, true),
                    null, null, textProps));
        }
    }

    private String getCurrentDisplayText() {
        if (cycleProps.values() == null || cycleProps.values().isEmpty()) return "?";
        CycleValue cv = cycleProps.values().get(currentIndex);
        return cv.textKey();
    }

    private static boolean hasTextContent(TextProps tp) {
        if (tp.text() != null && !tp.text().isEmpty() && !tp.text().trim().isEmpty()) return true;
        if (tp.textKey() != null && !tp.textKey().isEmpty()) return true;
        if (tp.textKeyDynamic() != null && !tp.textKeyDynamic().isEmpty()) return true;
        if (tp.textKeyOption() != null && !tp.textKeyOption().isEmpty()) return true;
        return false;
    }

    private void cycleValue(int delta, RenderContext ctx) {
        if (cycleProps.values() == null || cycleProps.values().isEmpty()) return;
        int size = cycleProps.values().size();
        currentIndex = (currentIndex + delta + size) % size;
        String key = cycleProps.values().get(currentIndex).key();
        this.member.put("current_value", getCurrentDisplayText());
        this.member.put("current_index", String.valueOf(currentIndex));
        this.member.put("current_key", key);
        if (stateKey != null && ctx != null && ctx.sharedState() != null) {
            ctx.sharedState().put(stateKey, key);
        }
        syncToOptions();
        playValueChangeSound();
    }

    private void syncToOptions() {
        if (cycleProps.optionKey() != null && !cycleProps.values().isEmpty()) {
            OptionBinder.setCycleOptionValue(cycleProps.optionKey(), cycleProps.values().get(currentIndex).key(), minecraft.options);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        String texPath = getSprite();
        if (texPath != null && !texPath.isEmpty()) {
            var id = parseTexturePath(texPath);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                NineSliceHelper.NineSliceInfo nineSlice = NineSliceHelper.loadNineSlice(id);
                if (nineSlice != null) {
                    NineSliceHelper.blitNineSliced(graphics, id, screenX, screenY, dim.w, dim.h, nineSlice);
                } else {
                    graphics.blit(id, screenX, screenY, 0, 0, dim.w, dim.h, dim.w, dim.h);
                }
            }
        } else {
            int bgColor = isHovered ? highlightedBackgroundColor : backgroundColor;
            graphics.fill(screenX, screenY, screenX + dim.w, screenY + dim.h, bgColor);
        }

        RenderContext childCtx = mergedCtx;
        childCtx = childCtx.withVar("current_value", getCurrentDisplayText());
        childCtx = childCtx.withVar("current_index", String.valueOf(currentIndex));
        childCtx = childCtx.withVar("current_key", cycleProps.values().isEmpty() ? "" : cycleProps.values().get(currentIndex).key());
        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, childCtx, mouseX, mouseY, delta);
        }

        if (!cycleProps.values().isEmpty()) {
            CycleValue cv = cycleProps.values().get(currentIndex);
            Component text = Component.translatable(cv.textKey());
            int textW = minecraft.font.width(text);
            int textX = screenX + (dim.w - textW) / 2;
            int textY = screenY + (dim.h - 8) / 2;
            int textColor = textProps != null ? BackgroundRenderer.parseColor(textProps.color()) : 0xFFFFFFFF;
            graphics.drawString(minecraft.font, text, textX, textY, textColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        if (mouseX >= screenX && mouseX <= screenX + dim.w && mouseY >= screenY && mouseY <= screenY + dim.h) {
            playClickSound();
            cycleValue(1, context);
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY, RenderContext context,
                             int x, int y, int width, int height) {
        WidgetDimension dim = computeLayout(mergeContext(context), width, height);
        int screenX = x + dim.x;
        int screenY = y + dim.y;
        boolean wasHovered = this.isHovered;
        isHovered = mouseX >= screenX && mouseX <= screenX + dim.w && mouseY >= screenY && mouseY <= screenY + dim.h;
        if (!wasHovered && this.isHovered) {
            playHoverSound();
        }
    }

    private String getSprite() {
        if (texture == null) return null;
        if (!layout.enabled()) {
            String t = texture.getDisabled();
            return t != null ? t : texture.getNormal();
        }
        if (isHovered) {
            String t = texture.getHighlighted();
            return t != null ? t : texture.getNormal();
        }
        return texture.getNormal();
    }
}