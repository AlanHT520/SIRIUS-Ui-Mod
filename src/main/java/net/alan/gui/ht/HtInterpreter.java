

package net.alan.gui.ht;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
public final class HtInterpreter {
    public record Result(
            HtValue.Obj panorama,
            List<HtValue> graphicsDraw,
            Map<String, String> sharedState,
            Map<String, String> variables,
            Map<String, String> member,
            List<HtValue> elements,
            List<String> warnings
    ) {
    }
    private static final Map<String, String> STATE_ALIAS = Map.of(
            "pressed", "ing",
            "hover", "highlighted",
            "ing", "ing",
            "normal", "normal",
            "highlighted", "highlighted",
            "disabled", "disabled",
            "selected", "selected"
    );
    private static final Set<String> STATE_EVENTS = Set.of(
            "normal", "highlighted", "hover", "pressed", "ing", "disabled", "selected"
    );
    private static final Map<String, List<String>> ACTION_POSITIONAL = Map.ofEntries(
            Map.entry("open_screen", List.of("screenId")),
            Map.entry("set_var", List.of("varName", "varValue")),
            Map.entry("show_toast", List.of("content")),
            Map.entry("open_link", List.of("url")),
            Map.entry("switch_box", List.of("boxId", "targetId")),
            Map.entry("switch_page", List.of("boxId", "targetId")),
            Map.entry("join_world", List.of("target")),
            Map.entry("delete_world", List.of("target")),
            Map.entry("edit_world", List.of("target")),
            Map.entry("recreate_world", List.of("target")),
            Map.entry("join_server", List.of("target")),
            Map.entry("delete_server", List.of("target")),
            Map.entry("edit_server", List.of("target")),
            Map.entry("delete_server_submit", List.of("target")),
            Map.entry("show_card", List.of("card")),
            Map.entry("card_submit", List.of()),
            Map.entry("card_cancel", List.of()),
            Map.entry("close_screen", List.of()),
            Map.entry("resume_game", List.of()),
            Map.entry("quit_game", List.of()),
            Map.entry("disconnect", List.of()),
            Map.entry("exit_to_title", List.of()),
            Map.entry("respawn", List.of()),
            Map.entry("refresh_servers", List.of()),
            Map.entry("commit_packs", List.of()),
            Map.entry("open_game_rules", List.of()),
            Map.entry("open_data_packs", List.of()),
            Map.entry("open_folder", List.of("target")),
            Map.entry("cycle_rule_value", List.of("target")),
            Map.entry("create_world", List.of()),
            Map.entry("add_server", List.of()),
            Map.entry("add_server_submit", List.of("content")),
            Map.entry("direct_connect", List.of()),
            Map.entry("direct_connect_submit", List.of("content")),
            Map.entry("server_input_submit", List.of("content")),
            Map.entry("edit_server_submit", List.of("content"))
    );
    private static final Set<String> ELEMENT_METHODS = Set.of(
            "position", "size", "text", "id", "option", "values", "range",
            "data", "row_height", "row", "var", "style", "padding", "use"
    );
    private final HtProgram program;
    private final Map<String, HtAst.MethodDecl> methods = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private final Out out = new Out();
    public HtInterpreter(HtProgram program) {
        this.program = program;
        this.methods.putAll(program.methods());
    }
    public static Result run(HtProgram program) {
        HtAst.ScreenDecl screen = program.rootScreen();
        if (screen == null) {
            throw new HtException(program.rootFile(), 0, 0,
                    "该 .ht 文件的根声明不是 screen（实际是 "
                            + program.rootUnit().main().getClass().getSimpleName() + "）");
        }
        return new HtInterpreter(program).interpret(screen);
    }
    public static HtValue.Obj runObject(HtProgram program) {
        if (!(program.rootUnit().main() instanceof HtAst.WidgetDecl decl)) {
            throw new HtException(program.rootFile(), 0, 0,
                    "该 .ht 文件的根声明不是 widget（对象文件需要 widget 根声明）");
        }
        return new HtInterpreter(program).interpretWidgetObject(decl);
    }
    private HtValue.Obj interpretWidgetObject(HtAst.WidgetDecl decl) {
        Env env = new Env(null);
        Frame screenFrame = Frame.screen(out);
        for (HtAst.Stmt stmt : program.globalVars()) {
            execStmt(stmt, env, screenFrame);
        }
        HtValue.Obj el = HtValue.obj();
        Frame frame = Frame.of(el);
        for (HtAst.WidgetDecl link : program.widgetChain(decl)) {
            Env bodyEnv = new Env(env);
            for (HtAst.Stmt stmt : link.body()) {
                execStmt(stmt, bodyEnv, frame);
            }
        }
        flushChildren(el, frame);
        return el;
    }
    public Result interpret(HtAst.ScreenDecl screen) {
        Env env = new Env(null);
        for (HtAst.Stmt stmt : program.globalVars()) {
            execStmt(stmt, env, Frame.screen(out));
        }
        for (HtAst.ScreenDecl decl : program.screenChain(screen)) {
            for (HtAst.Stmt stmt : decl.body()) {
                if (stmt instanceof HtAst.MethodDecl m) {
                    methods.put(m.name(), m);
                }
            }
            for (HtAst.Stmt stmt : decl.body()) {
                execStmt(stmt, env, Frame.screen(out));
            }
        }
        return new Result(out.panorama, out.graphicsDraw, out.sharedState,
                out.variables, out.member, out.elements, warnings);
    }
    private void execStmt(HtAst.Stmt stmt, Env env, Frame frame) {
        switch (stmt) {
            case HtAst.VarDecl v -> env.declare(v.name(), v.init() == null
                    ? defaultValue(v.type(), v.line()) : eval(v.init(), env, frame));
            case HtAst.MethodDecl m -> methods.put(m.name(), m);
            case HtAst.WidgetDefStmt ignored -> {
            }
            case HtAst.Assign a -> execAssign(a, env, frame);
            case HtAst.ExprStmt e -> execExprStmt(e, env, frame);
            case HtAst.If i -> {
                List<HtAst.Stmt> branch = eval(i.cond(), env, frame).truthy()
                        ? i.thenBody() : i.elseBody();
                for (HtAst.Stmt s : branch) {
                    execStmt(s, env, frame);
                }
            }
            case HtAst.For f -> execFor(f, env, frame);
            case HtAst.Block b -> {
                for (HtAst.Stmt s : b.body()) {
                    execStmt(s, env, frame);
                }
            }
            case HtAst.Return r -> throw new ReturnSignal(
                    r.value() == null ? HtValue.NULL : eval(r.value(), env, frame));
            case HtAst.Event ev -> execEvent(ev, env, frame);
            case HtAst.InstanceEvent ie -> {
                HtValue.Obj instance = env.instance(ie.instance());
                if (instance == null) {
                    throw HtException.at(program.rootFile(), ie.line(), 1,
                            "on 事件引用了未声明的实例：" + ie.instance());
                }
                execEvent(new HtAst.Event(ie.event(), List.of(), ie.body(), ie.line()),
                        env, Frame.of(instance));
            }
            case HtAst.StateBlock sb -> execStateBlock(sb, env, frame);
            case HtAst.SoundBlock sb -> execSoundBlock(sb, env, frame);
            case HtAst.SoundEntry se -> execSoundEntry(se, env, frame);
            case HtAst.AnimateBlock ab -> execAnimateBlock(ab, env, frame);
            case HtAst.AnimateEntry ignored -> {
            }
            case HtAst.PropsBlock pb -> execPropsBlock(pb, env, frame);
            case HtAst.InstanceProps ip -> {
                HtValue.Obj instance = env.instance(ip.instance());
                if (instance == null) {
                    throw HtException.at(program.rootFile(), ip.line(), 1,
                            "属性块引用了未声明的实例：" + ip.instance());
                }
                applyPropsToElement(instance, ip.key(), ip.body(), env, Frame.of(instance));
            }
            case HtAst.Background bg -> execBackground(bg, env, frame);
            case HtAst.Panorama p -> out.panorama = propsObject(p.body(), env, frame, "panorama");
            case HtAst.GraphicsDraw gd -> out.graphicsDraw.add(graphicsDraw(gd, env, frame));
            case HtAst.SharedState ss -> collectStringMap(ss.body(), env, frame, out.sharedState,
                    "shared_state");
            case HtAst.Instance inst -> {
                HtValue.Obj el = buildInstance(inst, env, frame);
                env.instances.put(inst.name(), el);
            }
            case HtAst.ImportStmt ignored -> {
            }
        }
    }
    private void execAssign(HtAst.Assign a, Env env, Frame frame) {
        HtValue value = eval(a.value(), env, frame);
        switch (a.target()) {
            case HtAst.Name n -> {
                if (env.has(n.name())) {
                    env.set(n.name(), value);
                } else if (frame.el != null) {
                    putElementProp(frame.el, n.name(), value);
                } else {
                    applyScreenProp(n.name(), value, a.line());
                }
            }
            case HtAst.Member m -> {
                if (m.target() instanceof HtAst.Name tn && "this".equals(tn.name())) {
                    if (frame.el == null) {
                        throw HtException.at(program.rootFile(), a.line(), 1,
                                "this." + m.name() + " 只能出现在 widget / 实例体内");
                    }
                    putThisProp(frame.el, m.name(), value);
                } else if (m.target() instanceof HtAst.Name inst
                        && env.instance(inst.name()) != null) {
                    HtValue.Obj el = env.instance(inst.name());
                    putThisProp(el, m.name(), value);
                } else {
                    throw HtException.at(program.rootFile(), a.line(), 1,
                            "无法赋值的左值：" + describe(m.target()));
                }
            }
            default -> throw HtException.at(program.rootFile(), a.line(), 1,
                    "无法赋值的左值：" + describe(a.target()));
        }
    }
    private void applyScreenProp(String key, HtValue value, int line) {
        switch (key) {
            case "graphicsDraw" -> {
                if (!(value instanceof HtValue.Arr arr)) {
                    throw HtException.at(program.rootFile(), line, 1,
                            "graphicsDraw 需要是数组");
                }
                out.graphicsDraw.clear();
                out.graphicsDraw.addAll(arr.items());
            }
            case "panorama" -> {
                if (!(value instanceof HtValue.Obj obj)) {
                    throw HtException.at(program.rootFile(), line, 1,
                            "panorama 需要是对象");
                }
                out.panorama = obj;
            }
            case "variables" -> collectStringMapFromObject(value, out.variables, line, key);
            case "member" -> collectStringMapFromObject(value, out.member, line, key);
            case "sharedState", "shared_state" ->
                    collectStringMapFromObject(value, out.sharedState, line, key);
            default -> throw HtException.at(program.rootFile(), line, 1,
                    "未知的屏幕级字段：" + key
                            + "（可用：panorama / graphicsDraw / variables / member / sharedState）");
        }
    }
    private void collectStringMapFromObject(HtValue value, Map<String, String> target,
                                            int line, String what) {
        if (!(value instanceof HtValue.Obj obj)) {
            throw HtException.at(program.rootFile(), line, 1, what + " 需要是对象");
        }
        for (Map.Entry<String, HtValue> entry : obj.fields().entrySet()) {
            target.put(entry.getKey(), actionString(entry.getValue()));
        }
    }
    private void execExprStmt(HtAst.ExprStmt stmt, Env env, Frame frame) {        HtAst.Expr expr = stmt.expr();
        if (expr instanceof HtAst.Call call) {
            HtValue.Obj targetEl = callTarget(call, env, frame);
            if (targetEl != null) {
                applyElementMethod(targetEl, call, env, frame);
                return;
            }
            if (call.target() == null) {
                if ("use".equals(call.name())) {
                    useElement(frame, evalUseTarget(call, env, frame), call);
                    return;
                }
                if (rewriteStateMethod(call, env, frame)) {
                    return;
                }
            }
        }
        eval(expr, env, frame);
    }
    private boolean rewriteStateMethod(HtAst.Call call, Env env, Frame frame) {
        return false;
    }
    private HtValue.Obj callTarget(HtAst.Call call, Env env, Frame frame) {
        HtAst.Expr target = call.target();
        if (target == null) {
            return null;
        }
        if (target instanceof HtAst.Name n) {
            if ("this".equals(n.name())) {
                if (frame.el == null) {
                    throw HtException.at(program.rootFile(), call.line(), 1,
                            "this." + call.name() + "(...) 只能出现在 widget / 实例体内");
                }
                return frame.el;
            }
            if (env.instance(n.name()) != null) {
                return env.instance(n.name());
            }
        }
        return null;
    }
    private void execFor(HtAst.For f, Env env, Frame frame) {
        HtValue source;
        try {
            source = eval(f.source(), env, frame);
        } catch (UnknownName unknown) {
            throw HtException.at(program.rootFile(), f.line(), 1,
                    "for 的循环对象必须在构建期可枚举，'" + unknown.getMessage()
                            + "' 属于运行期数据。数据源（world_saves / servers / realms / "
                            + "game_rules / resource_packs）请改用 dynamic_list 或 pack_list 控件。");
        }
        if (!(source instanceof HtValue.Arr arr)) {
            throw HtException.at(program.rootFile(), f.line(), 1,
                    "for 的数据来源必须是数组；如果这里写的是数据源（world_saves、servers ...），"
                            + "请改用 dynamic_list 控件——数据源是运行期数据，构建期无法枚举。实际是 "
                            + source.typeName());
        }
        List<HtValue> items = arr.items();
        for (int i = 0; i < items.size(); i++) {
            Env child = new Env(env);
            child.declare(f.itemName(), items.get(i));
            if (f.indexName() != null) {
                child.declare(f.indexName(), HtValue.num(i));
            }
            for (HtAst.Stmt s : f.body()) {
                execStmt(s, child, frame);
            }
        }
    }
    private void execEvent(HtAst.Event event, Env env, Frame frame) {
        String name = event.name().toLowerCase(Locale.ROOT);
        if (STATE_EVENTS.contains(name)) {
            HtValue.Obj stateV = stateVObject(frame.el);
            HtValue.Obj state = stateObject(stateV, name);
            for (HtAst.Stmt stmt : event.body()) {
                if (stmt instanceof HtAst.Assign a && a.target() instanceof HtAst.Name n) {
                    state.fields().put(n.name(), eval(a.value(), env, frame));
                } else {
                    warnings.add("state 事件块 on " + name + " 里只支持「属性 = 值」，已忽略 "
                            + stmt.getClass().getSimpleName());
                }
            }
            return;
        }
        if ("changed".equals(name) || "change".equals(name) || "value_changed".equals(name)) {
            warnings.add("on " + name + " 暂不支持（现有控件用 state_key + OptionBinder 处理值变化）");
            return;
        }
        if ("focus".equals(name) || "blur".equals(name) || "submit".equals(name)) {
            warnings.add("on " + name + " 暂不支持（现有控件没有对应回调），已忽略");
            return;
        }
        HtAst.Stmt first = event.body().isEmpty() ? null : event.body().get(0);
        if (!(first instanceof HtAst.ExprStmt es) || !(es.expr() instanceof HtAst.Call call)) {
            throw HtException.at(program.rootFile(), event.line(), 1,
                    "on " + event.name() + " { } 里只能写一个动作调用，例如 open_screen(\"worldSelect\")");
        }
        if (frame.el == null) {
            throw HtException.at(program.rootFile(), event.line(), 1,
                    "on 事件必须写在 widget / 实例体内");
        }
        frame.el.fields().put("action", buildAction(call, env, frame));
    }
    private HtValue.Obj buildAction(HtAst.Call call, Env env, Frame frame) {
        String type = call.name();
        HtValue.Obj action = HtValue.obj();
        action.fields().put("type", HtValue.str(type));
        List<String> positional = ACTION_POSITIONAL.get(type);
        if (positional == null) {
            warnings.add("未知 action 名称：" + type + "（已原样写入，ActionExecutor 可能忽略）");
            positional = List.of();
        }
        int posIndex = 0;
        for (HtAst.Arg arg : call.args()) {
            if (arg.name() != null) {
                putActionNamed(action, arg.name(), arg.value(), env, frame);
                continue;
            }
            if (posIndex < positional.size()) {
                String field = positional.get(posIndex);
                HtValue v = eval(arg.value(), env, frame);
                action.fields().put(field, HtValue.str(actionString(v)));
            } else if (posIndex == 0 && positional.isEmpty()) {
                // 未登记的动作：首个位置参数兜底写进 target（ActionExecutor 最通用的字段）
                action.fields().put("target",
                        HtValue.str(actionString(eval(arg.value(), env, frame))));
            } else {
                warnings.add("action " + type + " 的多余位置参数已忽略：" + describe(arg.value()));
            }
            posIndex++;
        }
        return action;
    }
    private void putActionNamed(HtValue.Obj action, String name, HtAst.Expr expr,
                                Env env, Frame frame) {
        String field = switch (name) {
            case "screen", "screen_id" -> "screenId";
            case "confirm_with" -> "confirm_with";
            default -> name;
        };
        HtValue v = eval(expr, env, frame);
        if ("card".equals(field)) {
            action.fields().put("card", v);
        } else {
            action.fields().put(field, HtValue.str(actionString(v)));
        }
    }
    private static String actionString(HtValue v) {
        if (v instanceof HtValue.Color c) {
            return String.format("0x%08X", c.argb());
        }
        return v.asString();
    }
    private void applyElementMethod(HtValue.Obj el, HtAst.Call call, Env env, Frame frame) {
        String name = call.name();
        List<HtAst.Arg> args = call.args();
        switch (name) {
            case "use" -> {
                if (args.isEmpty()) {
                    useElement(frame, el, call);
                } else {
                    HtValue folded = tryEval(args.get(0).value(), env, frame);
                    if (folded != null) {
                        if (folded.truthy()) {
                            useElement(frame, el, call);
                        }
                    } else {
                        el.fields().put("condition",
                                HtValue.str(exprString(args.get(0).value(), env, frame)));
                        useElement(frame, el, call);
                    }
                }
            }
            case "position" -> {
                HtValue.Obj pos = childObject(el, "position");
                pos.fields().put("x", layoutValue(arg(args, 0, name, call), env, frame));
                pos.fields().put("y", layoutValue(arg(args, 1, name, call), env, frame));
            }
            case "size" -> {
                HtValue.Obj size = childObject(el, "size");
                size.fields().put("width", layoutValue(arg(args, 0, name, call), env, frame));
                size.fields().put("height", layoutValue(arg(args, 1, name, call), env, frame));
            }
            case "text" -> applyText(el, args, env, frame, call);
            case "id" -> el.fields().put("id", HtValue.str(actionString(
                    eval(arg(args, 0, name, call), env, frame))));
            case "option" -> el.fields().put("option_key", HtValue.str(optionKey(
                    arg(args, 0, name, call), env, frame)));
            case "values" -> el.fields().put("values", cycleValues(args, env, frame, call));
            case "range" -> {
                el.fields().put("slider_min", eval(arg(args, 0, name, call), env, frame));
                el.fields().put("slider_max", eval(arg(args, 1, name, call), env, frame));
                if (args.size() > 2) {
                    el.fields().put("slider_step", eval(args.get(2).value(), env, frame));
                }
            }
            case "data" -> el.fields().put("data_source", HtValue.str(
                    optionKey(arg(args, 0, name, call), env, frame)));
            case "row_height" -> el.fields().put("row_height",
                    eval(arg(args, 0, name, call), env, frame));
            case "row" -> {
                HtValue.Arr rows = childArray(el, "rows");
                HtValue.Obj row = HtValue.obj();
                row.fields().put("height", eval(arg(args, 0, name, call), env, frame));
                if (args.size() > 1) {
                    row.fields().put("filter_key", HtValue.str(
                            eval(args.get(1).value(), env, frame).asString()));
                }
                rows.items().add(row);
            }
            case "var" -> {
                HtValue.Obj vars = childObject(el, "variables");
                vars.fields().put(arg(args, 0, name, call) instanceof HtAst.Lit lit
                                ? lit.value().asString() : describe(arg(args, 0, name, call)),
                        eval(arg(args, 1, name, call), env, frame));
            }
            case "style" -> el.fields().put("type", HtValue.str(
                    eval(arg(args, 0, name, call), env, frame).asString()));
            case "padding" -> el.fields().put("padding",
                    eval(arg(args, 0, name, call), env, frame));
            default -> throw HtException.at(program.rootFile(), call.line(), 1,
                    "this." + name + "(...) 不是受支持的方法；受支持的是 " + ELEMENT_METHODS
                            + "。其余字段请直接写 `字段名 = 值;` 或 `字段名 { ... }`。");
        }
    }
    private void applyText(HtValue.Obj el, List<HtAst.Arg> args, Env env, Frame frame,
                           HtAst.Call call) {
        if (args.isEmpty()) {
            throw HtException.at(program.rootFile(), call.line(), 1,
                    "this.text(...) 至少需要一个参数");
        }
        HtAst.Expr first = args.get(0).value();
        if (first instanceof HtAst.Member m && m.target() instanceof HtAst.Name ns
                && "options".equals(ns.name())) {
            el.fields().put("text_key.option", HtValue.str(snakeToCamel(m.name())));
            return;
        }
        String value = evalStringish(first, env, frame);
        if (args.size() == 1) {
            el.fields().put("text", HtValue.str(value));
            return;
        }
        boolean translate = eval(args.get(1).value(), env, frame).truthy();
        if (translate) {
            el.fields().put("text_key", HtValue.str(value));
            if (args.size() > 2) {
                HtValue.Arr translationArgs = HtValue.arr();
                for (int i = 2; i < args.size(); i++) {
                    translationArgs.items().add(HtValue.str(
                            exprString(args.get(i).value(), env, frame)));
                }
                el.fields().put("translation_args", translationArgs);
            }
        } else {
            el.fields().put("text", HtValue.str(value));
            if (args.size() > 2) {
                warnings.add("this.text(str, false, ...) 的额外参数没有意义，已忽略");
            }
        }
    }
    private String optionKey(HtAst.Expr expr, Env env, Frame frame) {
        if (expr instanceof HtAst.Member m && m.target() instanceof HtAst.Name ns
                && "options".equals(ns.name())) {
            return snakeToCamel(m.name());
        }
        HtValue v = eval(expr, env, frame);
        return snakeToCamel(v.asString());
    }
    private HtValue cycleValues(List<HtAst.Arg> args, Env env, Frame frame, HtAst.Call call) {
        HtValue.Arr values = HtValue.arr();
        for (HtAst.Arg arg : args) {
            HtValue.Obj entry = HtValue.obj();
            if (arg.value() instanceof HtAst.ArrLit pair && pair.items().size() == 2) {
                entry.fields().put("key", HtValue.str(exprString(pair.items().get(0), env, frame)));
                entry.fields().put("text_key",
                        HtValue.str(exprString(pair.items().get(1), env, frame)));
            } else if (arg.name() != null) {
                entry.fields().put("key", HtValue.str(arg.name()));
                entry.fields().put("text_key",
                        HtValue.str(exprString(arg.value(), env, frame)));
            } else {
                String key = exprString(arg.value(), env, frame);
                entry.fields().put("key", HtValue.str(key));
                entry.fields().put("text_key", HtValue.str(key));
            }
            values.items().add(entry);
        }
        return values;
    }
    private void useElement(Frame frame, HtValue.Obj el, HtAst.Call call) {
        if (frame.sink == null) {
            throw HtException.at(program.rootFile(), call.line(), 1,
                    "use() 只能出现在 screen 或 widget 体内");
        }
        addElement(frame.sink, el);
    }
    private HtValue.Obj evalUseTarget(HtAst.Call call, Env env, Frame frame) {
        throw HtException.at(program.rootFile(), call.line(), 1,
                "use() 必须写成 <实例名>.use()");
    }
    private void putElementProp(HtValue.Obj el, String name, HtValue value) {
        el.fields().put(name, normalizeValue(name, value));
    }
    private void putThisProp(HtValue.Obj el, String name, HtValue value) {
        switch (name) {
            case "positionX", "x" -> childObject(el, "position").fields()
                    .put("x", value);
            case "positionY", "y" -> childObject(el, "position").fields()
                    .put("y", value);
            case "width" -> childObject(el, "size").fields().put("width", value);
            case "height" -> childObject(el, "size").fields().put("height", value);
            default -> el.fields().put(name, normalizeValue(name, value));
        }
    }
    private HtValue normalizeValue(String key, HtValue value) {
        if (value instanceof HtValue.Color c) {
            return HtValue.str(String.format("0x%08X", c.argb()));
        }
        return value;
    }
    private void execPropsBlock(HtAst.PropsBlock pb, Env env, Frame frame) {
        if (frame.el == null) {
            switch (pb.key()) {
                case "variables" -> collectStringMap(pb.body(), env, frame, out.variables,
                        "screen.variables");
                case "member" -> collectStringMap(pb.body(), env, frame, out.member,
                        "screen.member");
                case "texture", "textures", "text" -> throw HtException.at(
                        program.rootFile(), pb.line(), 1,
                        pb.key() + " { } 只能出现在 widget / 实例体内");
                default -> out.panorama = propsObject(pb.body(), env, frame, pb.key());
            }
            return;
        }
        String key = "textures".equals(pb.key()) ? "texture" : pb.key();
        applyPropsToElement(frame.el, key, pb.body(), env, frame);
    }
    private void applyPropsToElement(HtValue.Obj el, String key, List<HtAst.Stmt> body,
                                     Env env, Frame frame) {
        HtValue.Obj target = childObject(el, key);
        HtValue.Obj incoming = propsObject(body, env, frame, key);
        if ("texture".equals(key)) {
            applyTextureAlias(incoming);
        }
        target.fields().putAll(incoming.fields());
    }
    private void applyTextureAlias(HtValue.Obj texture) {
        Map<String, HtValue> fields = texture.fields();
        List<String> names = new ArrayList<>(fields.keySet());
        for (String name : names) {
            String mapped = STATE_ALIAS.get(name.toLowerCase(Locale.ROOT));
            if (mapped != null && !mapped.equals(name)) {
                HtValue value = fields.remove(name);
                fields.put(mapped, value);
            }
        }
    }
    private HtValue.Obj propsObject(List<HtAst.Stmt> body, Env env, Frame frame, String key) {
        HtValue.Obj obj = HtValue.obj();
        for (HtAst.Stmt stmt : body) {
            switch (stmt) {
                case HtAst.Assign a -> {
                    String field = assignName(a.target());
                    obj.fields().put(field, normalizeValue(field, eval(a.value(), env, frame)));
                }
                case HtAst.PropsBlock pb -> {
                    String field = pb.key();
                    obj.fields().put(field, propsObject(pb.body(), env, frame, field));
                }
                case HtAst.ExprStmt es -> {
                    if (es.expr() instanceof HtAst.Call call && call.target() == null) {
                        obj.fields().put(call.name(), bareCallValue(call, env, frame));
                    } else {
                        warnings.add(key + " { } 里忽略了无法识别的语句");
                    }
                }
                case HtAst.VarDecl ignored -> {
                }
                default -> warnings.add(key + " { } 里忽略了 "
                        + stmt.getClass().getSimpleName());
            }
        }
        return obj;
    }
    private HtValue bareCallValue(HtAst.Call call, Env env, Frame frame) {
        if (call.args().isEmpty()) {
            return HtValue.TRUE;
        }
        if (call.args().size() == 1) {
            return eval(call.args().get(0).value(), env, frame);
        }
        HtValue.Arr arr = HtValue.arr();
        for (HtAst.Arg arg : call.args()) {
            arr.items().add(eval(arg.value(), env, frame));
        }
        return arr;
    }
    private void collectStringMap(List<HtAst.Stmt> body, Env env, Frame frame,
                                  Map<String, String> target, String what) {
        for (HtAst.Stmt stmt : body) {
            if (stmt instanceof HtAst.Assign a) {
                target.put(assignName(a.target()),
                        actionString(eval(a.value(), env, frame)));
            } else if (stmt instanceof HtAst.PropsBlock pb) {
                target.put(pb.key(), actionString(propsObject(pb.body(), env, frame, pb.key())));
            } else {
                warnings.add(what + " 里忽略了 " + stmt.getClass().getSimpleName());
            }
        }
    }
    private void execStateBlock(HtAst.StateBlock sb, Env env, Frame frame) {
        if (frame.el == null) {
            throw HtException.at(program.rootFile(), sb.line(), 1,
                    "state(...) 只能出现在 widget / 实例体内");
        }
        HtValue.Obj stateV = stateVObject(frame.el);
        for (HtAst.Stmt stmt : sb.body()) {
            switch (stmt) {
                case HtAst.PropsBlock pb -> {
                    if ("texture".equals(pb.key()) || "textures".equals(pb.key())) {
                        HtValue.Obj texture = childObject(frame.el, "texture");
                        for (HtAst.Stmt inner : pb.body()) {
                            if (inner instanceof HtAst.Assign a) {
                                String stateName = assignName(a.target());
                                texture.fields().put(stateKey(stateName),
                                        normalizeValue("texture", eval(a.value(), env, frame)));
                            } else {
                                warnings.add("state.textures 里忽略了 "
                                        + inner.getClass().getSimpleName());
                            }
                        }
                    } else {
                        for (HtAst.Stmt inner : pb.body()) {
                            if (!(inner instanceof HtAst.Assign a)) {
                                warnings.add("state." + pb.key() + " 里忽略了 "
                                        + inner.getClass().getSimpleName());
                                continue;
                            }
                            String stateName = assignName(a.target());
                            stateObject(stateV, stateName).fields().put(pb.key(),
                                    normalizeValue(pb.key(), eval(a.value(), env, frame)));
                        }
                    }
                }
                case HtAst.Assign a -> {
                    String field = assignName(a.target());
                    HtValue value = normalizeValue(field, eval(a.value(), env, frame));
                    for (String stateName : sb.states()) {
                        stateObject(stateV, stateName).fields().put(field, value);
                    }
                }
                case HtAst.ExprStmt es -> {
                    if (!(es.expr() instanceof HtAst.Call call) || call.target() != null) {
                        warnings.add("state 块里忽略了无法识别的语句");
                        continue;
                    }
                    for (String stateName : sb.states()) {
                        HtValue.Obj state = stateObject(stateV, stateName);
                        switch (call.name()) {
                            case "position" -> {
                                HtValue.Obj pos = childObject(state, "position");
                                pos.fields().put("x", layoutValue(call.args().get(0).value(),
                                        env, frame));
                                pos.fields().put("y", layoutValue(call.args().get(1).value(),
                                        env, frame));
                            }
                            case "size" -> {
                                HtValue.Obj size = childObject(state, "size");
                                size.fields().put("width", layoutValue(call.args().get(0).value(),
                                        env, frame));
                                size.fields().put("height", layoutValue(call.args().get(1).value(),
                                        env, frame));
                            }
                            default -> warnings.add("state 块里暂不支持 " + call.name() + "(...)");
                        }
                    }
                }
                default -> warnings.add("state 块里忽略了 " + stmt.getClass().getSimpleName());
            }
        }
    }
    private HtValue.Obj stateVObject(HtValue.Obj el) {
        return childObject(el, "state_v");
    }
    private HtValue.Obj stateObject(HtValue.Obj stateV, String stateName) {
        return childObject(stateV, stateKey(stateName));
    }
    private String stateKey(String stateName) {
        String mapped = STATE_ALIAS.get(stateName.toLowerCase(Locale.ROOT));
        return mapped != null ? mapped : stateName;
    }
    private void execSoundBlock(HtAst.SoundBlock sb, Env env, Frame frame) {
        if (frame.el == null) {
            throw HtException.at(program.rootFile(), sb.line(), 1, "sound { } 只能出现在控件体内");
        }
        HtValue.Obj sound = childObject(frame.el, "sound");
        for (HtAst.Stmt stmt : sb.body()) {
            if (stmt instanceof HtAst.SoundEntry entry) {
                execSoundEntry(entry, env, frame, sound);
            }
        }
    }
    private void execSoundEntry(HtAst.SoundEntry entry, Env env, Frame frame) {
        execSoundEntry(entry, env, frame, frame.el == null
                ? HtValue.obj() : childObject(frame.el, "sound"));
    }
    private void execSoundEntry(HtAst.SoundEntry entry, Env env, Frame frame,
                                HtValue.Obj sound) {
        HtValue.Obj config = HtValue.obj();
        if (entry.soundId() != null) {
            config.fields().put("id", entry.soundId());
        }
        for (HtAst.Stmt stmt : entry.config()) {
            if (stmt instanceof HtAst.Assign a) {
                String field = assignName(a.target());
                config.fields().put(field, eval(a.value(), env, frame));
            }
        }
        sound.fields().put(entry.event(), config);
    }
    private void execAnimateBlock(HtAst.AnimateBlock ab, Env env, Frame frame) {
        if (frame.el == null) {
            throw HtException.at(program.rootFile(), ab.line(), 1,
                    "animate { } 只能出现在控件体内");
        }
        HtValue.Arr animations = childArray(frame.el, "animations");
        for (HtAst.Stmt stmt : ab.body()) {
            if (!(stmt instanceof HtAst.AnimateEntry entry)) {
                continue;
            }
            HtValue.Obj step = HtValue.obj();
            step.fields().put("type", HtValue.str(entry.type()));
            for (HtAst.Arg arg : entry.args()) {
                String field = arg.name() != null ? arg.name()
                        : ("value" + step.fields().size());
                step.fields().put(field, eval(arg.value(), env, frame));
            }
            animations.items().add(step);
        }
    }
    private void execBackground(HtAst.Background bg, Env env, Frame frame) {
        HtValue.Obj props = propsObject(bg.body(), env, frame, "background");
        switch (bg.type()) {
            case "image", "video" -> {
                HtValue.Obj panorama = HtValue.obj();
                panorama.fields().put("type", HtValue.str(bg.type()));
                HtValue texture = props.fields().get("path");
                if (texture == null) {
                    texture = props.fields().get("texture");
                }
                if (texture != null) {
                    panorama.fields().put("texture", texture);
                }
                for (Map.Entry<String, HtValue> e : props.fields().entrySet()) {
                    if (!e.getKey().equals("path") && !e.getKey().equals("texture")) {
                        panorama.fields().put(e.getKey(), e.getValue());
                    }
                }
                out.panorama = panorama;
            }
            case "vanilla" -> {
                HtValue.Obj panorama = HtValue.obj();
                panorama.fields().put("type", HtValue.str("vanilla"));
                out.panorama = panorama;
            }
            case "color" -> {
                HtValue.Obj layer = HtValue.obj();
                HtValue color = props.fields().get("value");
                layer.fields().put("x", HtValue.str("0"));
                layer.fields().put("y", HtValue.str("0"));
                layer.fields().put("width", HtValue.str("screen.width"));
                layer.fields().put("height", HtValue.str("screen.height"));
                layer.fields().put("color", color != null ? color : HtValue.str("0xFF000000"));
                out.graphicsDraw.add(layer);
            }
            default -> warnings.add("background(" + bg.type() + ") 暂不支持，已忽略");
        }
    }
    private HtValue graphicsDraw(HtAst.GraphicsDraw gd, Env env, Frame frame) {
        HtValue.Obj layer = HtValue.obj();
        List<HtAst.Arg> args = gd.args();
        String[] fields = {"x", "y", "width", "height", "color"};
        for (int i = 0; i < fields.length && i < args.size(); i++) {
            layer.fields().put(fields[i], layoutValue(args.get(i).value(), env, frame));
        }
        if ("image".equals(gd.kind()) && !layer.fields().containsKey("color")) {
            warnings.add("graphicsDraw(image, ...) 需要颜色参数，现有 BackgroundLayer 只支持纯色");
        }
        return layer;
    }
    private HtValue.Obj buildInstance(HtAst.Instance inst, Env env, Frame parent) {
        HtValue.Obj el = HtValue.obj();
        el.fields().put("type", HtValue.str(inst.type()));
        if (inst.name() != null && !inst.name().startsWith("_")) {
            el.fields().put("id", HtValue.str(inst.name()));
        }
        Frame frame = Frame.of(el);
        if (inst.template() != null) {
            HtAst.WidgetDecl template = program.widget(inst.template());
            if (template == null) {
                throw HtException.at(program.rootFile(), inst.line(), 1,
                        "找不到控件模板：" + inst.template());
            }
            if (!template.type().equals(inst.type())) {
                throw HtException.at(program.rootFile(), inst.line(), 1,
                        "类型不匹配：" + inst.name() + " 声明为 " + inst.type()
                                + "，模板 " + inst.template() + " 是 " + template.type());
            }
            for (HtAst.WidgetDecl decl : program.widgetChain(template)) {
                Env bodyEnv = new Env(env);
                for (HtAst.Stmt stmt : decl.body()) {
                    execStmt(stmt, bodyEnv, frame);
                }
            }
        }
        Env bodyEnv = new Env(env);
        for (HtAst.Stmt stmt : inst.body()) {
            execStmt(stmt, bodyEnv, frame);
        }
        flushChildren(el, frame);
        return el;
    }
    private void flushChildren(HtValue.Obj el, Frame frame) {
        if (frame.children != null && !frame.children.isEmpty()) {
            el.fields().put("children", new HtValue.Arr(frame.children));
        }
    }
    private void addElement(List<HtValue> sink, HtValue.Obj el) {
        HtValue idValue = el.fields().get("id");
        if (idValue != null) {
            String id = idValue.asString();
            for (int i = 0; i < sink.size(); i++) {
                if (sink.get(i) instanceof HtValue.Obj existing
                        && existing.fields().get("id") instanceof HtValue v
                        && id.equals(v.asString())) {
                    sink.set(i, el);
                    return;
                }
            }
        }
        sink.add(el);
    }
    private HtValue eval(HtAst.Expr expr, Env env, Frame frame) {
        return switch (expr) {
            case HtAst.Lit lit -> lit.value();
            case HtAst.Name name -> resolveName(name, env, frame);
            case HtAst.Member member -> resolveMember(member, env, frame);
            case HtAst.Unary unary -> switch (unary.op()) {
                case "!" -> HtValue.bool(!eval(unary.operand(), env, frame).truthy());
                case "-" -> negate(eval(unary.operand(), env, frame));
                default -> throw HtException.at(program.rootFile(), unary.line(), 1,
                        "未知一元运算符 " + unary.op());
            };
            case HtAst.Binary binary -> binary(binary, env, frame);
            case HtAst.Ternary t -> eval(t.cond(), env, frame).truthy()
                    ? eval(t.thenExpr(), env, frame) : eval(t.elseExpr(), env, frame);
            case HtAst.Call call -> evalCall(call, env, frame);
            case HtAst.ArrLit arr -> {
                HtValue.Arr out = HtValue.arr();
                for (HtAst.Expr item : arr.items()) {
                    out.items().add(eval(item, env, frame));
                }
                yield out;
            }
            case HtAst.ObjLit obj -> {
                HtValue.Obj out = HtValue.obj();
                for (Map.Entry<String, HtAst.Expr> entry : obj.fields().entrySet()) {
                    out.fields().put(entry.getKey(),
                            normalizeValue(entry.getKey(), eval(entry.getValue(), env, frame)));
                }
                yield out;
            }
        };
    }
    private HtValue resolveName(HtAst.Name name, Env env, Frame frame) {
        if ("this".equals(name.name())) {
            throw new UnknownName("this");
        }
        HtValue value = env.get(name.name());
        if (value != null) {
            return value;
        }
        throw new UnknownName(name.name());
    }
    private HtValue resolveMember(HtAst.Member member, Env env, Frame frame) {
        if (member.target() instanceof HtAst.Name target) {
            switch (target.name()) {
                case "this" -> {
                    if (frame.el != null) {
                        HtValue read = readElementProp(frame.el, member.name());
                        if (read != null) {
                            return read;
                        }
                    }
                    throw new UnknownName("this." + member.name());
                }
                case "state" -> {
                    String key = member.name();
                    String value = out.sharedState.get(key);
                    if (value == null) {
                        value = out.variables.get(key);
                    }
                    if (value != null) {
                        return HtValue.str(value);
                    }
                    throw new UnknownName("state." + key);
                }
                default -> {
                    HtValue.Obj instance = env.instance(target.name());
                    if (instance != null) {
                        HtValue read = readElementProp(instance, member.name());
                        if (read != null) {
                            return read;
                        }
                        throw new UnknownName(target.name() + "." + member.name());
                    }
                    throw new UnknownName(target.name() + "." + member.name());
                }
            }
        }
        throw new UnknownName(describe(member));
    }
    private HtValue readElementProp(HtValue.Obj el, String prop) {
        switch (prop) {
            case "width" -> {
                return readChild(el, "size", "width");
            }
            case "height" -> {
                return readChild(el, "size", "height");
            }
            case "x", "positionX" -> {
                return readChild(el, "position", "x");
            }
            case "y", "positionY" -> {
                return readChild(el, "position", "y");
            }
            default -> {
                return el.fields().get(prop);
            }
        }
    }
    private HtValue readChild(HtValue.Obj el, String child, String field) {
        if (el.fields().get(child) instanceof HtValue.Obj obj) {
            return obj.fields().get(field);
        }
        return null;
    }
    private HtValue binary(HtAst.Binary b, Env env, Frame frame) {
        if ("&&".equals(b.op())) {
            return HtValue.bool(eval(b.left(), env, frame).truthy()
                    && eval(b.right(), env, frame).truthy());
        }
        if ("||".equals(b.op())) {
            return HtValue.bool(eval(b.left(), env, frame).truthy()
                    || eval(b.right(), env, frame).truthy());
        }
        HtValue left = eval(b.left(), env, frame);
        HtValue right = eval(b.right(), env, frame);
        switch (b.op()) {
            case "+" -> {
                if (left instanceof HtValue.Str || right instanceof HtValue.Str) {
                    return HtValue.str(left.asString() + right.asString());
                }
                return numeric(left, right).add();
            }
            case "-" -> {
                return numeric(left, right).sub();
            }
            case "*" -> {
                return numeric(left, right).mul();
            }
            case "/" -> {
                return numeric(left, right).div();
            }
            case "%" -> {
                return numeric(left, right).mod();
            }
            case "==" -> {
                return HtValue.bool(equalsValue(left, right));
            }
            case "!=" -> {
                return HtValue.bool(!equalsValue(left, right));
            }
            case "<", ">", "<=", ">=" -> {
                int cmp = compare(left, right);
                return HtValue.bool(switch (b.op()) {
                    case "<" -> cmp < 0;
                    case ">" -> cmp > 0;
                    case "<=" -> cmp <= 0;
                    default -> cmp >= 0;
                });
            }
            case "&" -> {
                return HtValue.num(left.asLong() & right.asLong());
            }
            case "|" -> {
                return HtValue.num(left.asLong() | right.asLong());
            }
            default -> throw HtException.at(program.rootFile(), b.line(), 1,
                    "未知运算符 " + b.op());
        }
    }
    private static boolean equalsValue(HtValue a, HtValue b) {
        if (a.isNumeric() && b.isNumeric()) {
            return Double.compare(a.asDouble(), b.asDouble()) == 0;
        }
        return a.asString().equals(b.asString());
    }
    private static int compare(HtValue a, HtValue b) {
        if (a.isNumeric() && b.isNumeric()) {
            return Double.compare(a.asDouble(), b.asDouble());
        }
        return a.asString().compareTo(b.asString());
    }
    private record Arithmetic(boolean isFloat, double a, double b) {
        HtValue add() {
            return result(a + b);
        }
        HtValue sub() {
            return result(a - b);
        }
        HtValue mul() {
            return result(a * b);
        }
        HtValue div() {
            if (b == 0) {
                return HtValue.num(0);
            }
            return result(a / b);
        }
        HtValue mod() {
            if (b == 0) {
                return HtValue.num(0);
            }
            return result(a % b);
        }
        private HtValue result(double v) {
            return isFloat ? HtValue.real(v) : HtValue.num(Math.round(v));
        }
    }
    private static Arithmetic numeric(HtValue left, HtValue right) {
        boolean isFloat = left instanceof HtValue.Real || right instanceof HtValue.Real;
        return new Arithmetic(isFloat, left.asDouble(), right.asDouble());
    }
    private static HtValue negate(HtValue v) {
        return v instanceof HtValue.Real ? HtValue.real(-v.asDouble())
                : HtValue.num(-v.asLong());
    }
    private HtValue evalCall(HtAst.Call call, Env env, Frame frame) {
        if (call.target() != null) {
            HtValue.Obj target = callTarget(call, env, frame);
            if (target != null) {
                throw HtException.at(program.rootFile(), call.line(), 1,
                        call.name() + "(...) 是「元素修改」语句，不能出现在表达式里");
            }
            throw HtException.at(program.rootFile(), call.line(), 1,
                    "不支持的调用： " + describe(call.target()) + "." + call.name() + "(...)");
        }
        if (ELEMENT_METHODS.contains(call.name()) && frame.el != null
                && !methods.containsKey(call.name())) {
            applyElementMethod(frame.el, call, env, frame);
            return HtValue.NULL;
        }
        HtAst.MethodDecl method = methods.get(call.name());
        if (method == null) {
            throw HtException.at(program.rootFile(), call.line(), 1,
                    "未定义的方法：" + call.name() + "(...)");
        }
        return invoke(method, call, env, frame);
    }
    private HtValue invoke(HtAst.MethodDecl method, HtAst.Call call, Env env, Frame frame) {
        if (method.params().size() != call.args().size()) {
            throw HtException.at(program.rootFile(), call.line(), 1,
                    "方法 " + method.name() + " 需要 " + method.params().size()
                            + " 个参数，实际传了 " + call.args().size() + " 个");
        }
        Env child = new Env(env);
        for (int i = 0; i < method.params().size(); i++) {
            child.declare(method.params().get(i).name(),
                    eval(call.args().get(i).value(), env, frame));
        }
        try {
            for (HtAst.Stmt stmt : method.body()) {
                execStmt(stmt, child, frame);
            }
        } catch (ReturnSignal signal) {
            return signal.value;
        }
        return HtValue.NULL;
    }
    private HtValue layoutValue(HtAst.Expr expr, Env env, Frame frame) {
        HtValue folded = tryEval(expr, env, frame);
        if (folded != null) {
            return folded instanceof HtValue.Color c
                    ? HtValue.str(String.format("0x%08X", c.argb())) : folded;
        }
        return HtValue.str(exprString(expr, env, frame));
    }
    private String exprString(HtAst.Expr expr, Env env, Frame frame) {
        HtValue folded = tryEval(expr, env, frame);
        if (folded != null) {
            return folded.exprString();
        }
        return switch (expr) {
            case HtAst.Lit lit -> lit.value().exprString();
            case HtAst.Name name -> name.name();
            case HtAst.Member member -> exprString(member.target(), env, frame)
                    + "." + member.name();
            case HtAst.Binary b -> "(" + exprString(b.left(), env, frame) + " "
                    + b.op() + " " + exprString(b.right(), env, frame) + ")";
            case HtAst.Unary u -> "!".equals(u.op())
                    ? "(!" + exprString(u.operand(), env, frame) + ")"
                    : "(0 - " + exprString(u.operand(), env, frame) + ")";
            case HtAst.Ternary t -> "(" + exprString(t.cond(), env, frame) + " ? "
                    + exprString(t.thenExpr(), env, frame) + " : "
                    + exprString(t.elseExpr(), env, frame) + ")";
            case HtAst.Call call -> {
                HtAst.MethodDecl method = methods.get(call.name());
                if (method != null) {
                    throw HtException.at(program.rootFile(), call.line(), 1,
                            "方法 " + call.name() + " 的返回值无法在运行期表达式里求值，"
                                    + "请在构建期用常量或变量算出结果");
                }
                throw HtException.at(program.rootFile(), call.line(), 1,
                        "表达式里的调用无法序列化：" + call.name() + "(...)");
            }
            case HtAst.ArrLit arr -> "[" + arr.items().size() + " items]";
            case HtAst.ObjLit obj -> "{" + obj.fields().size() + " fields}";
        };
    }
    private HtValue tryEval(HtAst.Expr expr, Env env, Frame frame) {
        try {
            return eval(expr, env, frame);
        } catch (UnknownName unknown) {
            return null;
        }
    }
    private String evalStringish(HtAst.Expr expr, Env env, Frame frame) {
        HtValue folded = tryEval(expr, env, frame);
        if (folded != null) {
            return folded.asString();
        }
        return exprString(expr, env, frame);
    }
    private static HtAst.Expr arg(List<HtAst.Arg> args, int index, String method,
                                  HtAst.Call call) {
        if (index >= args.size()) {
            throw new HtException("this." + method + "(...) 缺少第 " + (index + 1) + " 个参数");
        }
        return args.get(index).value();
    }
    private static HtValue.Obj childObject(HtValue.Obj owner, String key) {
        HtValue existing = owner.fields().get(key);
        if (existing instanceof HtValue.Obj obj) {
            return obj;
        }
        HtValue.Obj created = HtValue.obj();
        owner.fields().put(key, created);
        return created;
    }
    private static HtValue.Arr childArray(HtValue.Obj owner, String key) {
        HtValue existing = owner.fields().get(key);
        if (existing instanceof HtValue.Arr arr) {
            return arr;
        }
        HtValue.Arr created = HtValue.arr();
        owner.fields().put(key, created);
        return created;
    }
    private static String assignName(HtAst.Expr target) {
        return switch (target) {
            case HtAst.Name n -> n.name();
            case HtAst.Member m -> m.name();
            default -> throw new HtException("无法作为属性名：" + describe(target));
        };
    }
    private static String describe(HtAst.Expr expr) {
        return switch (expr) {
            case HtAst.Name n -> n.name();
            case HtAst.Member m -> describe(m.target()) + "." + m.name();
            case HtAst.Lit l -> l.value().asString();
            case HtAst.Call c -> c.name() + "(...)";
            default -> expr.getClass().getSimpleName();
        };
    }
    private HtValue defaultValue(String type, int line) {
        if (type == null) {
            return HtValue.NULL;
        }
        return switch (type) {
            case "int" -> HtValue.num(0);
            case "float" -> HtValue.real(0);
            case "string" -> HtValue.str("");
            case "bool" -> HtValue.FALSE;
            case "color" -> new HtValue.Color(0);
            default -> HtValue.NULL;
        };
    }
    static String snakeToCamel(String s) {
        if (s == null || s.indexOf('_') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean upper = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    private static final class UnknownName extends RuntimeException {
        UnknownName(String name) {
            super(name, null, false, false);
        }
    }
    private static final class ReturnSignal extends RuntimeException {
        private final transient HtValue value;
        ReturnSignal(HtValue value) {
            super(null, null, false, false);
            this.value = value;
        }
    }
    private static final class Out {
        HtValue.Obj panorama;
        final List<HtValue> graphicsDraw = new ArrayList<>();
        final Map<String, String> sharedState = new LinkedHashMap<>();
        final Map<String, String> variables = new LinkedHashMap<>();
        final Map<String, String> member = new LinkedHashMap<>();
        final List<HtValue> elements = new ArrayList<>();
    }
    private static final class Frame {
        final HtValue.Obj el;
        final List<HtValue> sink;
        final List<HtValue> children;
        private Frame(HtValue.Obj el, List<HtValue> sink, List<HtValue> children) {
            this.el = el;
            this.sink = sink;
            this.children = children;
        }
        static Frame screen(Out out) {
            return new Frame(null, out.elements, null);
        }
        static Frame of(HtValue.Obj el) {
            List<HtValue> children = new ArrayList<>();
            return new Frame(el, children, children);
        }
    }
    private static final class Env {
        private final Env parent;
        private final Map<String, HtValue> vars = new LinkedHashMap<>();
        private final Map<String, HtValue.Obj> instances = new LinkedHashMap<>();
        Env(Env parent) {
            this.parent = parent;
        }
        void declare(String name, HtValue value) {
            vars.put(name, value);
        }
        boolean has(String name) {
            return vars.containsKey(name) || (parent != null && parent.has(name));
        }
        HtValue get(String name) {
            HtValue local = vars.get(name);
            if (local != null) {
                return local;
            }
            return parent != null ? parent.get(name) : null;
        }
        void set(String name, HtValue value) {
            if (vars.containsKey(name)) {
                vars.put(name, value);
            } else if (parent != null && parent.has(name)) {
                parent.set(name, value);
            } else {
                vars.put(name, value);
            }
        }
        HtValue.Obj instance(String name) {
            HtValue.Obj local = instances.get(name);
            if (local != null) {
                return local;
            }
            return parent != null ? parent.instance(name) : null;
        }
    }
}
