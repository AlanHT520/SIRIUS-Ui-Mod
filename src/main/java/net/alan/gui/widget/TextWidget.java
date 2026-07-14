package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.AnimationStep;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.TextProps;
import net.alan.gui.render.BackgroundRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextWidget extends BaseWidget {
    private final TextProps textProps;
    private final Minecraft minecraft;
    private long animStartMillis = -1;
    private long seqStartMillis = -1;
    private int lastVisibleChars = -1;

    public TextWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member, TextProps textProps) {
        super(id, layout, variables, member);
        this.textProps = textProps;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;

        // 获取最终文本
        String finalText = null;
        Component finalComponent = null;
        String dynamic = textProps.dynamicType();

        // 优先使用 text_key.d（动态翻译，Component 渲染）
        String dynKey = evalStringExpr(vars, textProps.textKeyDynamic());
        if (dynKey != null && !dynKey.isEmpty()) {
            String replacedKey = replaceVars(dynKey, mergedCtx.variables());
            List<String> replacedArgs;
            if (textProps.translationArgs() != null) {
                replacedArgs = new ArrayList<>();
                for (String arg : textProps.translationArgs()) {
                    String resolved = replaceVars(arg, mergedCtx.variables());
                    resolved = resolvePlaceholder(resolved, dynamic, mergedCtx);
                    replacedArgs.add(resolved);
                }
            } else {
                // 未提供 translation_args，自动从上下文收集动态变量值
                replacedArgs = collectTranslationArgs(mergedCtx);
            }
            finalComponent = Component.translatable(replacedKey, replacedArgs.toArray());
        } else if (textProps.textKey() != null && !textProps.textKey().isEmpty()
                && textProps.textKeyOption() != null && !textProps.textKeyOption().isEmpty()) {
            // text_key + text_key.option 同时存在 → "FOV: 70" 格式（对标原版 genericValueLabel）
            String replacedKey = replaceVars(evalStringExpr(vars, textProps.textKey()), mergedCtx.variables());
            Component caption = Component.translatable(replacedKey);
            String currentValue = mergedCtx.variables().get("current_value");
            Component value = currentValue != null ? Component.translatable(currentValue) : Component.literal("?");
            finalComponent = Component.translatable("options.generic_value", caption, value);
        } else if (textProps.textKeyOption() != null && !textProps.textKeyOption().isEmpty()) {
            // text_key.option 单独使用：渲染选项当前值（如 "70"）
            String currentValue = mergedCtx.variables().get("current_value");
            if (currentValue != null) {
                finalComponent = Component.translatable(currentValue);
            }
        } else if (dynamic != null && !dynamic.isEmpty()) {
            // 无 text_key.d 时，dynamicType 直接输出值
            finalText = resolveDynamicValue(dynamic);
        } else {
            String rawKey = evalStringExpr(vars, textProps.textKey());
            if (rawKey != null && !rawKey.isEmpty()) {
                String replacedKey = replaceVars(rawKey, mergedCtx.variables());
                List<String> replacedArgs = null;
                if (textProps.translationArgs() != null) {
                    replacedArgs = new ArrayList<>();
                    for (String arg : textProps.translationArgs()) {
                        String resolved = replaceVars(arg, mergedCtx.variables());
                        resolved = resolvePlaceholder(resolved, dynamic, mergedCtx);
                        replacedArgs.add(resolved);
                    }
                }
                Component comp = Component.translatable(replacedKey,
                        replacedArgs != null ? replacedArgs.toArray() : new Object[0]);
                finalText = comp.getString();
            } else {
                String raw = evalStringExpr(vars, textProps.text());
                if (raw != null) {
                    finalText = replaceVars(raw, mergedCtx.variables());
                }
            }
        }

        if (finalText == null && finalComponent == null) return;
        if (finalText != null && finalText.isEmpty()) return;

        // 计算文本实际尺寸（考虑缩放，state_v 可覆盖 scale）
        float scale = textProps.scale();
        String scaleOverride = getStateProp(mergedCtx, "scale");
        if (scaleOverride != null) {
            try { scale = Float.parseFloat(scaleOverride); } catch (NumberFormatException ignored) {}
        }
        int textWidth;
        if (finalComponent != null) {
            textWidth = (int) (minecraft.font.width(finalComponent.getVisualOrderText()) * scale);
        } else {
            textWidth = (int) (minecraft.font.width(finalText) * scale);
        }
        int textHeight = (int) (minecraft.font.lineHeight * scale);

        // 构建数值变量上下文
        Map<String, Integer> numVars = new HashMap<>();
        numVars.put("screen.width", mergedCtx.screenWidth());
        numVars.put("screen.height", mergedCtx.screenHeight());
        numVars.put("parent.width", width);
        numVars.put("parent.height", height);
        numVars.put("this.width", textWidth);
        numVars.put("this.height", textHeight);
        // 合并上下文字符串变量中的数值
        for (Map.Entry<String, String> entry : mergedCtx.variables().entrySet()) {
            try {
                numVars.put(entry.getKey(), Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException ignored) {}
        }

        // 解析位置（使用 state_v 覆盖后的有效表达式）
        int posX = eval(getEffectiveXExpr(mergedCtx), numVars);
        int posY = eval(getEffectiveYExpr(mergedCtx), numVars);
        int screenX = x + posX;
        int screenY = y + posY;

        // 解析颜色
        String colorStr = textProps.color();
        if (colorStr != null) colorStr = replaceVars(colorStr, mergedCtx.variables());
        int color = BackgroundRenderer.parseColor(colorStr);

        // 文本动画 — 优先使用 animations 序列，其次兼容单个 text_animation
        List<AnimationStep> steps = textProps.animations();
        String fullStr = finalComponent != null ? finalComponent.getString() : finalText;

        if (steps != null && !steps.isEmpty()) {
            if (seqStartMillis < 0) seqStartMillis = System.currentTimeMillis();
            long totalElapsed = System.currentTimeMillis() - seqStartMillis;

            long cursor = 0;
            AnimationStep currentStep = null;
            long stepLocalElapsed = 0;
            boolean allDone = true;
            int simVisibleChars = -1;

            for (AnimationStep step : steps) {
                long stepDur = step.duration();
                if ("typewriter".equals(step.type())) {
                    int s = step.start() >= 0 ? step.start() : Math.max(0, simVisibleChars);
                    int e = step.end() > 0 ? step.end() : fullStr.length();
                    int charsToType = Math.max(0, Math.min(e, fullStr.length()) - s);
                    if (step.speed() > 0) {
                        stepDur = (long) Math.ceil(charsToType * 1000.0 / step.speed());
                    } else if (stepDur <= 0) {
                        stepDur = (long) Math.ceil(charsToType * 1000.0 / 20.0);
                    }
                }
                long stepStart = cursor + step.delay();
                long stepEnd = stepStart + stepDur;
                if (totalElapsed < stepStart) {
                    allDone = false;
                    break;
                }
                if (totalElapsed < stepEnd || step.loop()) {
                    currentStep = step;
                    stepLocalElapsed = totalElapsed - stepStart;
                    if (step.loop() && stepDur > 0) {
                        stepLocalElapsed = stepLocalElapsed % stepDur;
                    }
                    allDone = false;
                    break;
                }
                if ("typewriter".equals(step.type())) {
                    int s = step.start() >= 0 ? step.start() : Math.max(0, simVisibleChars);
                    int e = step.end() > 0 ? step.end() : fullStr.length();
                    simVisibleChars = Math.min(e, fullStr.length());
                }
                cursor = stepEnd;
            }

            if (currentStep != null) {
                String renderText = fullStr;
                int renderColor = color;
                int renderX = screenX;
                int renderY = screenY;
                boolean showCursor = false;

                switch (currentStep.type()) {
                    case "typewriter" -> {
                        int startIdx = currentStep.start() >= 0 ? currentStep.start()
                                : Math.max(0, simVisibleChars);
                        int endIdx = currentStep.end() > 0 ? currentStep.end() : fullStr.length();
                        endIdx = Math.min(endIdx, fullStr.length());
                        int charsTotal = Math.max(0, endIdx - startIdx);
                        int charsToShow;
                        if (currentStep.speed() > 0) {
                            float twSpeed = currentStep.speed();
                            charsToShow = (int) (stepLocalElapsed * twSpeed / 1000.0f);
                        } else if (currentStep.duration() > 0) {
                            float progress = Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration());
                            charsToShow = (int) Math.ceil(charsTotal * progress);
                        } else {
                            charsToShow = (int) (stepLocalElapsed * 20.0f / 1000.0f);
                        }
                        charsToShow = Math.min(charsToShow, charsTotal);
                        lastVisibleChars = Math.min(startIdx + charsToShow, fullStr.length());
                        renderText = fullStr.substring(0, lastVisibleChars);
                        int tw = (int) (minecraft.font.width(renderText) * scale);
                        numVars.put("this.width", tw);
                        renderX = x + eval(getEffectiveXExpr(mergedCtx), numVars);
                        renderY = y + eval(getEffectiveYExpr(mergedCtx), numVars);
                        showCursor = (stepLocalElapsed / 500) % 2 == 0;
                    }
                    case "hold" -> {
                        int holdChars = lastVisibleChars >= 0 ? lastVisibleChars : fullStr.length();
                        renderText = fullStr.substring(0, holdChars);
                        int tw = (int) (minecraft.font.width(renderText) * scale);
                        numVars.put("this.width", tw);
                        renderX = x + eval(getEffectiveXExpr(mergedCtx), numVars);
                        renderY = y + eval(getEffectiveYExpr(mergedCtx), numVars);
                    }
                    case "fade_in" -> {
                        float progress = currentStep.duration() > 0
                                ? Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration()) : 1.0f;
                        int alpha = (int) (255 * progress);
                        renderColor = (alpha << 24) | (color & 0x00FFFFFF);
                    }
                    case "fade_out" -> {
                        float progress = currentStep.duration() > 0
                                ? Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration()) : 1.0f;
                        int alpha = (int) (255 * (1.0f - progress));
                        renderColor = (alpha << 24) | (color & 0x00FFFFFF);
                    }
                    case "slide_in_left" -> {
                        float progress = currentStep.duration() > 0
                                ? Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration()) : 1.0f;
                        int offset = (int) ((1.0f - progress) * 50);
                        renderX = screenX - offset;
                    }
                    case "slide_in_right" -> {
                        float progress = currentStep.duration() > 0
                                ? Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration()) : 1.0f;
                        int offset = (int) ((1.0f - progress) * 50);
                        renderX = screenX + offset;
                    }
                    case "slide_in_up" -> {
                        float progress = currentStep.duration() > 0
                                ? Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration()) : 1.0f;
                        int offset = (int) ((1.0f - progress) * 30);
                        renderY = screenY + offset;
                    }
                    case "slide_in_down" -> {
                        float progress = currentStep.duration() > 0
                                ? Math.min(1.0f, (float) stepLocalElapsed / currentStep.duration()) : 1.0f;
                        int offset = (int) ((1.0f - progress) * 30);
                        renderY = screenY - offset;
                    }
                    case "blink" -> {
                        showCursor = (stepLocalElapsed / 500) % 2 == 0;
                        if (!showCursor) return;
                    }
                    default -> {}
                }

                graphics.pose().pushPose();
                graphics.pose().translate(renderX, renderY, 0);
                if (scale != 1.0f) {
                    graphics.pose().scale(scale, scale, 1.0f);
                }
                graphics.drawString(minecraft.font, renderText, 0, 0, renderColor, textProps.shadow());
                if (showCursor) {
                    int cursorX = minecraft.font.width(renderText);
                    graphics.drawString(minecraft.font, "_", cursorX, 0, renderColor, textProps.shadow());
                }
                graphics.pose().popPose();
                return;
            }

            if (allDone) {
                seqStartMillis = -1;
                lastVisibleChars = -1;
            }
        } else if (textProps.textAnimation() != null && !textProps.textAnimation().isEmpty()) {
            // 兼容旧的单动画模式
            if (animStartMillis < 0) animStartMillis = System.currentTimeMillis();
            if (fullStr != null) {
                long elapsed = System.currentTimeMillis() - animStartMillis;
                if ("typewriter".equals(textProps.textAnimation())) {
                    float speed = textProps.animationSpeed() > 0 ? textProps.animationSpeed() : 20.0f;
                    int visibleChars = Math.min((int) (elapsed * speed / 1000.0f), fullStr.length());
                    if (visibleChars < fullStr.length()) {
                        String visibleText = fullStr.substring(0, visibleChars);
                        textWidth = (int) (minecraft.font.width(visibleText) * scale);
                        numVars.put("this.width", textWidth);
                        screenX = x + eval(getEffectiveXExpr(mergedCtx), numVars);
                        screenY = y + eval(getEffectiveYExpr(mergedCtx), numVars);

                        graphics.pose().pushPose();
                        graphics.pose().translate(screenX, screenY, 0);
                        if (scale != 1.0f) graphics.pose().scale(scale, scale, 1.0f);
                        graphics.drawString(minecraft.font, visibleText, 0, 0, color, textProps.shadow());
                        if ((elapsed / 500) % 2 == 0) {
                            graphics.drawString(minecraft.font, "_", minecraft.font.width(visibleText), 0, color, textProps.shadow());
                        }
                        graphics.pose().popPose();
                        return;
                    }
                    animStartMillis = -1;
                }
            }
        }

        // 正常绘制
        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0);
        if (scale != 1.0f) {
            graphics.pose().scale(scale, scale, 1.0f);
        }
        if (finalComponent != null) {
            graphics.drawString(minecraft.font, finalComponent, 0, 0, color, textProps.shadow());
        } else {
            graphics.drawString(minecraft.font, finalText, 0, 0, color, textProps.shadow());
        }
        graphics.pose().popPose();
    }

    /**
     * 如果 arg 是格式占位符（%s, %d），尝试用 dynamicType 或上下文动态变量解析
     */
    private String resolvePlaceholder(String arg, String dynamicType, RenderContext ctx) {
        if (!arg.matches("%[sd]")) return arg;

        // 用 dynamicType 解析
        if (dynamicType != null && !dynamicType.isEmpty()) {
            String resolved = resolveDynamicValue(dynamicType);
            if (resolved != null) return resolved;
        }

        // 从上下文变量中取第一个匹配的值
        for (String val : ctx.variables().values()) {
            if (val != null && !val.isEmpty()) return val;
        }

        return arg;
    }

    /**
     * 根据 dynamicType 获取实际值
     */
    private String resolveDynamicValue(String dynamicType) {
        return switch (dynamicType) {
            case "score" -> String.valueOf(minecraft.player != null ? minecraft.player.getScore() : 0);
            case "player_name" -> minecraft.player != null ? minecraft.player.getName().getString() : "Player";
            case "death_cause" -> minecraft.player != null ?
                    (minecraft.player.getLastDeathLocation() != null ? "Died" : "Alive") : "";
            default -> null;
        };
    }
}