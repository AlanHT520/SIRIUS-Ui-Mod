package net.alan.gui.widget;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.alan.gui.context.RenderContext;
import net.alan.gui.context.WidgetDimensionRegistry;
import net.alan.gui.data.DynamicListData;
import net.alan.gui.data.action.Action;
import net.alan.gui.data.props.DynamicListStyleConfig;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.factory.WidgetFactory;
import net.alan.gui.render.ActionExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

public class DynamicListWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicListWidget.class);
    private final Supplier<List<DynamicListData>> dataSource;
    private final ActionExecutor executor;
    private final int rowHeight;
    private final int gap;
    private final String backgroundColor;
    private final JsonObject rowTemplate;
    private final DynamicListStyleConfig styleConfig;
    private final JsonObject editBtnTemplate;
    private final JsonObject joinBtnTemplate;
    private final JsonObject deleteBtnTemplate;
    private List<DynamicListData> rows;
    private double scrollAmount;
    private boolean scrolling;
    private double lastMouseY;
    private int hoveredRowIndex = -1;
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();

    private final Map<Integer, ButtonWidget> joinButtons = new HashMap<>();
    private final Map<Integer, ButtonWidget> deleteButtons = new HashMap<>();
    private final Map<Integer, ButtonWidget> editButtons = new HashMap<>();

    public DynamicListWidget(String id, LayoutProps layout, Map<String, String> variables,
                             Map<String, String> member, Supplier<List<DynamicListData>> dataSource,
                             ActionExecutor executor, int rowHeight, int gap,
                             String backgroundColor,
                             JsonObject rowTemplate, DynamicListStyleConfig styleConfig,
                             JsonObject editBtnTemplate, JsonObject joinBtnTemplate,
                             JsonObject deleteBtnTemplate) {
        super(id, layout, variables, member);
        this.dataSource = dataSource;
        this.executor = executor;
        this.rowHeight = rowHeight;
        this.gap = gap;
        this.backgroundColor = backgroundColor;
        this.rowTemplate = rowTemplate;
        this.styleConfig = styleConfig;
        this.editBtnTemplate = editBtnTemplate;
        this.joinBtnTemplate = joinBtnTemplate;
        this.deleteBtnTemplate = deleteBtnTemplate;
        this.rows = new ArrayList<>();
        registerButtonTemplateDimensions();
        executor.addRefreshCallback(this::reload);
        reload();
    }

    public void reload() {
        rows = dataSource.get();
        scrollAmount = 0;
        iconCache.clear();
        rebuildButtons();
    }

    private void rebuildButtons() {
        joinButtons.clear();
        deleteButtons.clear();
        editButtons.clear();
        for (int i = 0; i < rows.size(); i++) {
            DynamicListData data = rows.get(i);
            if (joinBtnTemplate != null) {
                ButtonWidget btn = createButtonForRow(joinBtnTemplate, data, i, "join");
                if (btn != null) joinButtons.put(i, btn);
            }
            if (deleteBtnTemplate != null) {
                ButtonWidget btn = createButtonForRow(deleteBtnTemplate, data, i, "delete");
                if (btn != null) deleteButtons.put(i, btn);
            }
            if (editBtnTemplate != null && data.isJoinable()) {
                ButtonWidget btn = createButtonForRow(editBtnTemplate, data, i, "edit");
                if (btn != null) editButtons.put(i, btn);
            }
        }
    }

    private ButtonWidget createButtonForRow(JsonObject template, DynamicListData data, int rowIndex,
                                            String btnType) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();

            JsonObject btnJson = template.deepCopy();

            btnJson.addProperty("id", id + "_" + btnType + "_" + rowIndex);

            Action templateAction = template.has("action")
                    ? gson.fromJson(template.get("action"), Action.class)
                    : new Action();
            Action rowAction = buildAction(templateAction, data, btnType);
            btnJson.add("action", gson.toJsonTree(rowAction).getAsJsonObject());

            Widget widget = WidgetFactory.create(btnJson, null, executor);
            if (widget instanceof ButtonWidget btn) {
                return btn;
            }
            LOGGER.warn("Created widget is not a ButtonWidget: {}",
                    widget != null ? widget.getClass().getName() : "null");
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to create {} button for row {}: {}", btnType, rowIndex, e.getMessage());
            return null;
        }
    }

    private void registerButtonTemplateDimensions() {
        registerTemplateDim("join_button", joinBtnTemplate, 50, 18);
        registerTemplateDim("delete_button", deleteBtnTemplate, 36, 20);
        registerTemplateDim("edit_button", editBtnTemplate, 36, 20);
    }

    private void registerTemplateDim(String name, JsonObject template, int defaultW, int defaultH) {
        if (template == null) return;
        int w = defaultW;
        int h = defaultH;
        if (template.has("size") && template.get("size").isJsonObject()) {
            JsonObject sizeObj = template.get("size").getAsJsonObject();
            if (sizeObj.has("width")) {
                try { w = sizeObj.get("width").getAsInt(); } catch (Exception ignored) {}
            }
            if (sizeObj.has("height")) {
                try { h = sizeObj.get("height").getAsInt(); } catch (Exception ignored) {}
            }
        }
        WidgetDimensionRegistry.registerTemplate(name, new WidgetDimension(0, 0, w, h));
    }

    private Action buildAction(Action templateAction, DynamicListData data, String btnType) {
        Action action = new Action();
        action.setTarget(data.getId());

        switch (btnType) {
            case "join":
                action.setType(data.getActionType());
                break;
            case "delete":
                action.setType("delete_" + data.getActionType().replace("join_", ""));
                action.setContent(data.getName());
                break;
            case "edit":
                action.setType("edit_" + data.getActionType().replace("join_", ""));
                break;
            default:
                action.setType(templateAction.getType());
                break;
        }

        action.setScreenId(templateAction.getScreenId());
        action.setUrl(templateAction.getUrl());
        action.setVarName(templateAction.getVarName());
        action.setVarValue(templateAction.getVarValue());
        action.setBoxId(templateAction.getBoxId());
        action.setTargetId(templateAction.getTargetId());
        return action;
    }

    private int getTotalContentHeight() {
        if (rows.isEmpty()) return 0;
        int dividerH = (styleConfig != null && styleConfig.divider() != null)
                ? styleConfig.divider().height : 0;
        return rows.size() * rowHeight + (rows.size() - 1) * (gap + dividerH);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int listX = x + dim.x;
        int listY = y + dim.y;

        if (backgroundColor != null) {
            try {
                int color = (int) Long.parseLong(backgroundColor.replace("0x", ""), 16);
                graphics.fill(listX, listY, listX + dim.w, listY + dim.h, color);
            } catch (NumberFormatException ignored) {}
        }

        int totalH = getTotalContentHeight();
        double maxScroll = Math.max(0, totalH - dim.h);
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));

        graphics.enableScissor(listX, listY, listX + dim.w, listY + dim.h);

        Minecraft mc = Minecraft.getInstance();
        int currentY = listY - (int) scrollAmount;
        hoveredRowIndex = -1;

        DynamicListStyleConfig.RowStyleConfig rowStyle = styleConfig != null
                ? styleConfig.rowStyle() : null;
        DynamicListStyleConfig.DividerConfig dividerCfg = styleConfig != null
                ? styleConfig.divider() : null;
        int dividerH = dividerCfg != null ? dividerCfg.height : 0;
        int dividerColor = dividerCfg != null ? parseColor(dividerCfg.color) : 0;

        for (int i = 0; i < rows.size(); i++) {
            int rowTop = currentY;
            int rowBottom = currentY + rowHeight;

            if (rowBottom > listY && rowTop < listY + dim.h) {
                DynamicListData data = rows.get(i);

                boolean mouseOnRow = mouseX >= listX && mouseX <= listX + dim.w
                        && mouseY >= rowTop && mouseY <= rowBottom;
                if (mouseOnRow) hoveredRowIndex = i;

                int rowBgColor;
                if (mouseOnRow && rowStyle != null && rowStyle.hover_background_color != null) {
                    rowBgColor = parseColor(rowStyle.hover_background_color);
                } else if (rowStyle != null) {
                    rowBgColor = (i % 2 == 0)
                            ? parseColor(rowStyle.background_color)
                            : parseColor(rowStyle.background_color_alt);
                } else {
                    rowBgColor = (i % 2 == 0) ? 0x10000000 : 0x00000000;
                }
                if (rowBgColor != 0) {
                    graphics.fill(listX, rowTop, listX + dim.w, rowBottom, rowBgColor);
                }

                if (rowStyle != null && rowStyle.background_texture != null) {
                    ResourceLocation tex = ResourceLocation.tryParse(rowStyle.background_texture);
                    if (tex != null) {
                        graphics.blitSprite(tex, listX, rowTop, dim.w, rowHeight);
                    }
                }

                if (dividerH > 0 && dividerColor != 0 && i < rows.size() - 1) {
                    graphics.fill(listX, rowBottom, listX + dim.w, rowBottom + dividerH, dividerColor);
                }

                int iconX = getJsonInt(rowTemplate, "icon", "x", 4);
                int iconY = getJsonInt(rowTemplate, "icon", "y", 4);
                int iconW = getJsonInt(rowTemplate, "icon", "w", 32);
                int iconH = getJsonInt(rowTemplate, "icon", "h", 32);
                String iconPlaceholder = getJsonString(rowTemplate, "icon", "placeholder_color", "0xFF555555");

                ResourceLocation iconTex = getIconTexture(data.getIconPath());
                if (iconTex != null) {
                    RenderSystem.enableBlend();
                    graphics.blit(iconTex, listX + iconX, rowTop + iconY,
                            0, 0, iconW, iconH, iconW, iconH);
                    RenderSystem.disableBlend();
                } else {
                    int pc = parseColor(iconPlaceholder);
                    graphics.fill(listX + iconX, rowTop + iconY,
                            listX + iconX + iconW, rowTop + iconY + iconH, pc);
                }

                int nameX = getJsonInt(rowTemplate, "name", "x", 40);
                int nameY = getJsonInt(rowTemplate, "name", "y", 4);
                String nameColorStr = getJsonString(rowTemplate, "name", "color", null);
                int nameColor;
                if (nameColorStr != null) {
                    nameColor = parseColor(nameColorStr);
                } else if (rowStyle != null) {
                    nameColor = (i % 2 == 0)
                            ? parseColor(rowStyle.text_color)
                            : parseColor(rowStyle.text_color_alt);
                } else {
                    nameColor = 0xFFFFFFFF;
                }
                graphics.drawString(mc.font, data.getName(),
                        listX + nameX, rowTop + nameY, nameColor);

                if (data.getDescription() != null) {
                    int descX = getJsonInt(rowTemplate, "description", "x", 40);
                    int descY = getJsonInt(rowTemplate, "description", "y", 20);
                    String descColorStr = getJsonString(rowTemplate, "description", "color", null);
                    int descColor;
                    if (descColorStr != null) {
                        descColor = parseColor(descColorStr);
                    } else if (rowStyle != null) {
                        descColor = (i % 2 == 0)
                                ? parseColor(rowStyle.text_color_alt)
                                : parseColor(rowStyle.text_color);
                    } else {
                        descColor = 0xFFAAAAAA;
                    }
                    graphics.drawString(mc.font, data.getDescription(),
                            listX + descX, rowTop + descY, descColor);
                }

                ButtonWidget joinBtn = joinButtons.get(i);
                if (joinBtn != null) {
                    joinBtn.render(graphics, listX, rowTop, dim.w, rowHeight,
                            mergedCtx, mouseX, mouseY, delta);
                }
                ButtonWidget deleteBtn = deleteButtons.get(i);
                if (deleteBtn != null) {
                    deleteBtn.render(graphics, listX, rowTop, dim.w, rowHeight,
                            mergedCtx, mouseX, mouseY, delta);
                }
                ButtonWidget editBtn = editButtons.get(i);
                if (editBtn != null) {
                    editBtn.render(graphics, listX, rowTop, dim.w, rowHeight,
                            mergedCtx, mouseX, mouseY, delta);
                }
            }
            currentY += rowHeight + gap + dividerH;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            DynamicListStyleConfig.ScrollbarConfig sb = styleConfig != null
                    ? styleConfig.scrollbar() : null;
            int sbW = sb != null ? sb.width : 4;
            int sbX = listX + dim.w - sbW;
            int sbTrackColor = sb != null ? parseColor(sb.track_color) : 0x33000000;
            int sbThumbColor = sb != null ? parseColor(sb.thumb_color) : 0xAAFFFFFF;
            int barH = (int) (dim.h * dim.h / (dim.h + maxScroll));
            barH = Math.max(16, Math.min(barH, dim.h - 8));
            int barY = listY + (int) (scrollAmount * (dim.h - barH) / maxScroll);
            graphics.fill(sbX, listY, sbX + sbW, listY + dim.h, sbTrackColor);
            graphics.fill(sbX, barY, sbX + sbW, barY + barH, sbThumbColor);
        }
    }

    private static int parseColor(String colorStr) {
        try {
            return (int) Long.parseLong(colorStr.replace("0x", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    private ResourceLocation getIconTexture(String iconPath) {
        if (iconPath == null || iconPath.isEmpty()) {
            return null;
        }
        return iconCache.computeIfAbsent(iconPath, path -> {
            try {
                java.nio.file.Path iconFile = java.nio.file.Path.of(path);
                if (!java.nio.file.Files.exists(iconFile)) {
                    return null;
                }
                NativeImage iconImage = NativeImage.read(
                        java.nio.file.Files.newInputStream(iconFile));
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                String hash = Integer.toHexString(path.hashCode());
                ResourceLocation loc = ResourceLocation.withDefaultNamespace(
                        "dynamic_list_icon/" + hash);
                textureManager.register(loc, new DynamicTexture(iconImage));
                return loc;
            } catch (Exception e) {
                return null;
            }
        });
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
        if (mouseX < screenX || mouseX > screenX + dim.w
                || mouseY < screenY || mouseY > screenY + dim.h) {
            return false;
        }

        int currentY = screenY - (int) scrollAmount;
        int dividerH = (styleConfig != null && styleConfig.divider() != null)
                ? styleConfig.divider().height : 0;

        for (int i = 0; i < rows.size(); i++) {
            int rowTop = currentY;
            int rowBottom = currentY + rowHeight;

            if (rowBottom > screenY && rowTop < screenY + dim.h) {
                ButtonWidget joinBtn = joinButtons.get(i);
                if (joinBtn != null && joinBtn.mouseClicked(mouseX, mouseY, button,
                        mergedCtx, screenX, rowTop, dim.w, rowHeight)) {
                    return true;
                }
                ButtonWidget deleteBtn = deleteButtons.get(i);
                if (deleteBtn != null && deleteBtn.mouseClicked(mouseX, mouseY, button,
                        mergedCtx, screenX, rowTop, dim.w, rowHeight)) {
                    return true;
                }
                ButtonWidget editBtn = editButtons.get(i);
                if (editBtn != null && editBtn.mouseClicked(mouseX, mouseY, button,
                        mergedCtx, screenX, rowTop, dim.w, rowHeight)) {
                    return true;
                }
            }
            currentY += rowHeight + gap + dividerH;
        }

        if (button == 0) {
            scrolling = true;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        scrolling = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible()) return false;
        scrollAmount -= scrollY * 20;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY,
                                RenderContext context, int x, int y, int width, int height) {
        if (scrolling) {
            scrollAmount -= (mouseY - lastMouseY);
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    private static int getJsonInt(JsonObject parent, String key, String subKey, int defaultValue) {
        if (parent == null || !parent.has(key)) return defaultValue;
        JsonObject obj = parent.getAsJsonObject(key);
        if (obj == null || !obj.has(subKey)) return defaultValue;
        return obj.get(subKey).getAsInt();
    }

    private static String getJsonString(JsonObject parent, String key, String subKey,
                                        String defaultValue) {
        if (parent == null || !parent.has(key)) return defaultValue;
        JsonObject obj = parent.getAsJsonObject(key);
        if (obj == null || !obj.has(subKey)) return defaultValue;
        return obj.get(subKey).getAsString();
    }

    private static boolean getJsonBool(JsonObject parent, String key, String subKey,
                                       boolean defaultValue) {
        if (parent == null || !parent.has(key)) return defaultValue;
        JsonObject obj = parent.getAsJsonObject(key);
        if (obj == null || !obj.has(subKey)) return defaultValue;
        return obj.get(subKey).getAsBoolean();
    }
}