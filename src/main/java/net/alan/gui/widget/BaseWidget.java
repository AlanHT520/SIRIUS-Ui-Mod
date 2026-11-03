

package net.alan.gui.widget;
import net.alan.gui.context.RenderContext;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.context.WidgetDimensionRegistry;
import net.alan.gui.data.Action;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.SoundEventConfig;
import net.alan.gui.data.widget.WidgetSoundConfig;
import net.alan.gui.util.ExpressionEvaluator;
import net.alan.gui.util.GameStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public abstract class BaseWidget implements Widget {
    protected final String id;
    protected final LayoutProps layout;
    protected final Map<String, String> variables;
    protected final Map<String, String> member;
    protected Map<String, Map<String, Map<String, String>>> stateV;
    protected Map<String, Map<String, String>> stateVProps;
    protected WidgetSoundConfig soundConfig;
    private RenderContext currentRenderContext;
    private long lastValueChangeTick = -1;
    private long lastClickTick = -1;
    private SoundInstance activeValueChangeSound = null;
    protected BaseWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member) {
        this.id = id;
        this.layout = layout;
        this.variables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        this.member = member != null ? new HashMap<>(member) : new HashMap<>();
    }
    @Override
    public String getId() { return id; }
    protected String replaceVars(String template, Map<String, String> vars) {
        if (template == null) return null;
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
    protected int evalX(String expr, Map<String, Integer> numericVars) {
        if ("center".equals(expr)) {
            int parentW = numericVars.getOrDefault("parent.width", 0);
            int thisW = numericVars.getOrDefault("this.width", 0);
            return (parentW - thisW) / 2;
        }
        return eval(expr, numericVars);
    }
    protected int evalY(String expr, Map<String, Integer> numericVars) {
        if ("center".equals(expr)) {
            int parentH = numericVars.getOrDefault("parent.height", 0);
            int thisH = numericVars.getOrDefault("this.height", 0);
            return (parentH - thisH) / 2;
        }
        return eval(expr, numericVars);
    }
    protected int eval(String expr, Map<String, Integer> numericVars) {
        if (expr == null || expr.equals("auto")) return 0;
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            Integer direct = numericVars.get(expr);
            if (direct != null) return direct;
            if (!mightBeExpression(expr)) return 0;
            try {
                return ExpressionEvaluator.eval(
                        expr,
                        numericVars.getOrDefault("screen.width", 0),
                        numericVars.getOrDefault("screen.height", 0),
                        numericVars.getOrDefault("this.width", 0),
                        numericVars.getOrDefault("this.height", 0),
                        numericVars
                );
            } catch (RuntimeException ex) {
                return 0;
            }
        }
    }
    private static boolean mightBeExpression(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')' || c == '?' || c == ':' || c == '<' || c == '>' || c == '=' || c == '!' || c == '&' || c == '|') return true;
        }
        return false;
    }
    protected Map<String, Integer> buildNumericVars(RenderContext ctx, int parentW, int parentH, int thisW, int thisH) {
        Map<String, Integer> vars = new HashMap<>();
        vars.put("screen.width", ctx.screenWidth());
        vars.put("screen.height", ctx.screenHeight());
        vars.put("parent.width", parentW);
        vars.put("parent.height", parentH);
        vars.put("this.width", thisW);
        vars.put("this.height", thisH);
        scanWidgetDimensionRefs(vars, layout.widthExpr(), layout.heightExpr(),
                layout.xExpr(), layout.yExpr(), layout.condition());
        if (this.member != null) {
            scanWidgetDimensionRefs(vars, this.member.values().toArray(new String[0]));
        }
        Map<String, String> screenMembers = ctx.screenMembers();
        if (screenMembers != null && !screenMembers.isEmpty()) {
            for (Map.Entry<String, String> entry : screenMembers.entrySet()) {
                String expr = entry.getValue();
                int value = eval(expr, vars);
                vars.put("this." + entry.getKey(), value);
                if (!vars.containsKey(entry.getKey())) {
                    vars.put(entry.getKey(), value);
                }
            }
        }
        for (String screenId : ScreenVariableRegistry.getRegisteredScreenIds()) {
            Map<String, String> screenMember = ScreenVariableRegistry.getScreenMember(screenId);
            if (screenMember != null && !screenMember.isEmpty()) {
                for (Map.Entry<String, String> entry : screenMember.entrySet()) {
                    String expr = entry.getValue();
                    int value = eval(expr, vars);
                    String fullKey = screenId + "." + entry.getKey();
                    vars.put(fullKey, value);
                }
            }
            Map<String, Map<String, String>> elements = null;
            for (String elementId : ScreenVariableRegistry.getElementIds(screenId)) {
                Map<String, String> elementMember = ScreenVariableRegistry.getElementMember(screenId, elementId);
                if (elementMember != null && !elementMember.isEmpty()) {
                    for (Map.Entry<String, String> entry : elementMember.entrySet()) {
                        String expr = entry.getValue();
                        int value = eval(expr, vars);
                        String fullKey = screenId + "." + elementId + "." + entry.getKey();
                        vars.put(fullKey, value);
                    }
                }
            }
        }
        for (Map.Entry<String, String> entry : ctx.variables().entrySet()) {
            String key = entry.getKey();
            if (vars.containsKey(key)) continue;
            try {
                vars.put(key, Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException e) {
                int value = eval(entry.getValue(), vars);
                vars.put(key, value);
                if (!vars.containsKey("this." + key)) {
                    vars.put("this." + key, value);
                }
            }
        }
        if (this.member != null && !this.member.isEmpty()) {
            for (Map.Entry<String, String> entry : this.member.entrySet()) {
                String expr = entry.getValue();
                int value = eval(expr, vars);
                if (this.id != null) {
                    vars.put("this." + this.id + "." + entry.getKey(), value);
                }
                vars.put("this." + entry.getKey(), value);
                vars.put(entry.getKey(), value);
            }
        }
        injectGameStateVars(vars);
        injectSharedStateVars(ctx, vars);
        injectTextWidthDeferred(vars);
        return vars;
    }
    private void injectGameStateVars(Map<String, Integer> vars) {
        String[] gameKeys = {
            "game.in_level", "game.is_singleplayer", "game.is_multiplayer",
            "game.is_integrated_server", "game.is_dedicated_server",
            "game.is_hardcore", "game.is_flat", "game.is_debug", "game.is_demo",
            "game.is_paused", "game.is_creative", "game.is_survival",
            "game.is_spectator", "game.is_adventure", "game.game_mode",
            "game.difficulty", "game.is_hard_difficulty", "game.can_modify_difficulty",
            "game.is_difficulty_locked", "game.has_cheats", "game.is_host",
            "game.is_op", "game.is_connected", "game.is_realm", "game.player_count"
        };
        for (String key : gameKeys) {
            vars.put(key, GameStateProvider.resolve(key));
        }
    }
    private void injectSharedStateVars(RenderContext ctx, Map<String, Integer> vars) {
        Map<String, String> sharedState = ctx.sharedState();
        if (sharedState == null) return;
        for (Map.Entry<String, String> entry : sharedState.entrySet()) {
            String key = "state." + entry.getKey();
            if (vars.containsKey(key)) continue;
            try {
                vars.put(key, Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException e) {
                int value = eval(entry.getValue(), vars);
                vars.put(key, value);
            }
        }
    }
    private void injectTextWidthDeferred(Map<String, Integer> vars) {
        if (vars.containsKey("text_width")) return;
        if (this.variables == null) return;
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.font == null) return;
            String resolved = null;
            String textKey = this.variables.get("__text_key");
            if (textKey != null) {
                resolved = net.minecraft.network.chat.Component.translatable(textKey).getString();
            } else {
                String rawText = this.variables.get("__text_raw");
                if (rawText != null) {
                    resolved = rawText;
                }
            }
            if (resolved != null && !resolved.isEmpty()) {
                vars.put("text_width", mc.font.width(resolved));
                vars.put("text_height", mc.font.lineHeight);
            }
        } catch (Exception ignored) {
        }
    }
    private static final Pattern WIDGET_REF_PATTERN =
            Pattern.compile("\\b([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)+)\\b");
    private void scanWidgetDimensionRefs(Map<String, Integer> vars, String... exprs) {
        for (String expr : exprs) {
            if (expr == null) continue;
            Matcher m = WIDGET_REF_PATTERN.matcher(expr);
            while (m.find()) {
                String ref = m.group(1);
                if (vars.containsKey(ref)) continue;
                Integer value = WidgetDimensionRegistry.resolveProperty(ref);
                if (value != null) {
                    vars.put(ref, value);
                }
            }
        }
    }
    protected boolean checkCondition(Map<String, Integer> numericVars) {
        return checkCondition(numericVars, currentRenderContext);
    }
    protected boolean checkCondition(Map<String, Integer> numericVars, RenderContext ctx) {
        String condition = layout.condition();
        if (condition == null || condition.trim().isEmpty()) return true;
        if (condition.contains("'")) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\$\\{([^}]+)\\}\\s*(==|!=)\\s*'([^']*)'");
            java.util.regex.Matcher m = p.matcher(condition.trim());
            if (m.matches()) {
                String varName = m.group(1);
                String op = m.group(2);
                String expected = m.group(3);
                String actual = resolveStringVar(varName, ctx);
                boolean result = "==".equals(op) ? expected.equals(actual) : !expected.equals(actual);
                return result;
            }
        }
        try {
            int result = ExpressionEvaluator.eval(
                condition,
                numericVars.getOrDefault("screen.width", 0),
                numericVars.getOrDefault("screen.height", 0),
                numericVars.getOrDefault("this.width", 0),
                numericVars.getOrDefault("this.height", 0),
                numericVars
            );
            return result != 0;
        } catch (RuntimeException e) {
            return true;
        }
    }
    private String resolveStringVar(String varName, RenderContext ctx) {
        if (variables != null && variables.containsKey(varName)) {
            return variables.get(varName);
        }
        if (member != null && member.containsKey(varName)) {
            return member.get(varName);
        }
        if (ctx != null) {
            if (ctx.sharedState() != null) {
                String val = ctx.sharedState().get(varName);
                if (val != null) return val;
            }
            if (ctx.variables() != null) {
                String val = ctx.variables().get(varName);
                if (val != null) return val;
            }
        }
        return null;
    }
    protected Action resolveAction(Action a, Map<String, Integer> vars) {
        if (a == null) return null;
        String resolvedType = evalStringExpr(vars, a.getType());
        String resolvedScreenId = evalStringExpr(vars, a.getScreenId());
        boolean typeSame = (resolvedType == null) ? (a.getType() == null) : resolvedType.equals(a.getType());
        boolean screenIdSame = (resolvedScreenId == null) ? (a.getScreenId() == null) : resolvedScreenId.equals(a.getScreenId());
        if (typeSame && screenIdSame) {
            return a;
        }
        Action resolved = new Action();
        resolved.setType(resolvedType);
        resolved.setScreenId(resolvedScreenId);
        resolved.setUrl(a.getUrl());
        resolved.setTarget(a.getTarget());
        resolved.setContent(a.getContent());
        resolved.setVarName(a.getVarName());
        resolved.setVarValue(a.getVarValue());
        resolved.setBoxId(a.getBoxId());
        resolved.setTargetId(a.getTargetId());
        return resolved;
    }
    protected String evalStringExpr(Map<String, Integer> numericVars, String expr) {
        return ExpressionEvaluator.evalString(
            expr,
            numericVars.getOrDefault("screen.width", 0),
            numericVars.getOrDefault("screen.height", 0),
            numericVars.getOrDefault("this.width", 0),
            numericVars.getOrDefault("this.height", 0),
            numericVars
        );
    }
    protected RenderContext mergeContext(RenderContext ctx) {
        if ((variables == null || variables.isEmpty()) && (member == null || member.isEmpty())) {
            this.currentRenderContext = ctx;
            return ctx;
        }
        RenderContext result = ctx.copy();
        if (member != null) result.putAllVars(member);
        if (variables != null) result.putAllVars(variables);
        this.currentRenderContext = result;
        return result;
    }
    @Override
    public RenderContext mergeRenderContext(RenderContext ctx) {
        return mergeContext(ctx);
    }
    public void setStateV(Map<String, Map<String, Map<String, String>>> stateV) {
        this.stateV = stateV;
    }
    public void setStateVProps(Map<String, Map<String, String>> stateVProps) {
        this.stateVProps = stateVProps;
    }
    protected String getStateProp(RenderContext context, String key) {
        if (stateVProps == null) return null;
        String buttonState = context.variables().get("_button_state");
        if (buttonState == null) return null;
        Map<String, String> props = stateVProps.get(buttonState);
        if (props == null) return null;
        return props.get(key);
    }
    protected String getEffectiveXExpr(RenderContext context) {
        if (stateV == null) return layout.xExpr();
        String buttonState = context.variables().get("_button_state");
        if (buttonState == null) return layout.xExpr();
        Map<String, Map<String, String>> stateOverrides = stateV.get(buttonState);
        if (stateOverrides == null) return layout.xExpr();
        Map<String, String> posOverrides = stateOverrides.get("position");
        if (posOverrides == null) return layout.xExpr();
        String x = posOverrides.get("x");
        return x != null ? x : layout.xExpr();
    }
    protected String getEffectiveYExpr(RenderContext context) {
        if (stateV == null) return layout.yExpr();
        String buttonState = context.variables().get("_button_state");
        if (buttonState == null) return layout.yExpr();
        Map<String, Map<String, String>> stateOverrides = stateV.get(buttonState);
        if (stateOverrides == null) return layout.yExpr();
        Map<String, String> posOverrides = stateOverrides.get("position");
        if (posOverrides == null) return layout.yExpr();
        String y = posOverrides.get("y");
        return y != null ? y : layout.yExpr();
    }
    protected List<String> collectTranslationArgs(RenderContext ctx) {
        List<String> args = new ArrayList<>();
        List<String> keys = new ArrayList<>(ctx.variables().keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            if (key.startsWith("slider_")) continue;
            String value = ctx.variables().get(key);
            if (value != null && !value.isEmpty()) {
                args.add(value);
            }
        }
        return args;
    }
    public WidgetDimension computeLayout(RenderContext ctx, int parentW, int parentH) {
        Map<String, Integer> numVars = buildNumericVars(ctx, parentW, parentH, parentW, parentH);
        String wExpr = layout.widthExpr();
        String hExpr = layout.heightExpr();
        int w = (wExpr == null || wExpr.equals("auto")) ? parentW : eval(wExpr, numVars);
        int h = (hExpr == null || hExpr.equals("auto")) ? parentH : eval(hExpr, numVars);
        numVars.put("this.width", w);
        numVars.put("this.height", h);
        String xExpr = layout.xExpr();
        String yExpr = layout.yExpr();
        int x = evalX(xExpr, numVars);
        int y = evalY(yExpr, numVars);
        return new WidgetDimension(x, y, w, h);
    }
    public static class WidgetDimension {
        public final int x, y, w, h;
        public WidgetDimension(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
    public void setSoundConfig(WidgetSoundConfig soundConfig) {
        this.soundConfig = soundConfig;
    }
    public WidgetSoundConfig getSoundConfig() {
        return soundConfig;
    }
    protected SoundInstance playSound(SoundEventConfig sound) {
        if (sound == null || !sound.isValid()) return null;
        try {
            SoundInstance instance = SimpleSoundInstance.forUI(
                sound.getSoundEvent().value(), sound.getPitch(), sound.getVolume()
            );
            Minecraft.getInstance().getSoundManager().play(instance);
            return instance;
        } catch (Exception e) {
            return null;
        }
    }
    private long getCurrentTick() {
        return System.currentTimeMillis() / 50;
    }
    private boolean isThrottled(long lastTick, int throttleTicks) {
        if (throttleTicks <= 0) return false;
        long currentTick = getCurrentTick();
        if (lastTick > currentTick) return false;
        return lastTick >= 0 && (currentTick - lastTick) < throttleTicks;
    }
    protected void playClickSound() {
        if (soundConfig != null) {
            if (soundConfig.hasClick()) {
                playSound(soundConfig.getClick());
            }
        } else {
            try {
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
            } catch (Exception e) {
            }
        }
    }
    protected void playHoverSound() {
        if (soundConfig != null && soundConfig.hasHover()) {
            playSound(soundConfig.getHover());
        }
    }
    protected void playValueChangeSound() {
        if (soundConfig == null || !soundConfig.hasValueChange()) return;
        SoundEventConfig sound = soundConfig.getValueChange();
        if (isThrottled(lastValueChangeTick, sound.getThrottleTicks())) return;
        lastValueChangeTick = getCurrentTick();
        activeValueChangeSound = playSound(sound);
    }
    protected void stopValueChangeSound() {
        if (activeValueChangeSound != null) {
            try {
                Minecraft.getInstance().getSoundManager().stop(activeValueChangeSound);
            } catch (Exception e) {
            }
            activeValueChangeSound = null;
        }
    }
    protected void playDragStartSound() {
        if (soundConfig != null && soundConfig.hasDragStart()) {
            playSound(soundConfig.getDragStart());
        }
    }
    protected void playDragEndSound() {
        if (soundConfig != null && soundConfig.hasDragEnd()) {
            playSound(soundConfig.getDragEnd());
        }
    }
    protected void playFocusSound() {
        if (soundConfig != null && soundConfig.hasFocus()) {
            playSound(soundConfig.getFocus());
        }
    }
    protected void playReleaseSound() {
        if (soundConfig != null && soundConfig.hasRelease()) {
            playSound(soundConfig.getRelease());
        }
    }
}
