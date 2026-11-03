

package net.alan.gui.ht;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
public final class HtProgram {
    public interface SourceLoader {
        String load(String fileId) throws IOException;
    }
    public record ScreenEntry(HtAst.ScreenDecl decl, String fileId) {
    }
    private final String rootFile;
    private final HtAst.Unit rootUnit;
    private final Map<String, HtAst.Unit> units = new LinkedHashMap<>();
    private final Map<String, HtAst.WidgetDecl> widgets = new LinkedHashMap<>();
    private final Map<String, String> widgetOwner = new LinkedHashMap<>();
    private final Map<String, HtAst.MethodDecl> methods = new LinkedHashMap<>();
    private final List<HtAst.Stmt> globalVars = new ArrayList<>();
    private final Map<String, ScreenEntry> screens = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> register = new LinkedHashMap<>();
    private final Set<String> visiting = new HashSet<>();
    /** import 展开期收集到的错误：坏文件会被跳过，其余屏幕照常可用。 */
    private final List<String> errors = new ArrayList<>();
    private HtProgram(String rootFile, HtAst.Unit rootUnit) {
        this.rootFile = rootFile;
        this.rootUnit = rootUnit;
    }
    public static HtProgram load(String rootFile, SourceLoader loader) {
        String content;
        try {
            content = loader.load(rootFile);
        } catch (IOException e) {
            throw new HtException(rootFile, 0, 0, "读取失败：" + e.getMessage(), e);
        }
        if (content == null) {
            throw new HtException(rootFile, 0, 0, "文件不存在");
        }
        HtAst.Unit root = HtParser.parse(rootFile, content);
        HtProgram program = new HtProgram(rootFile, root);
        program.visit(rootFile, root, loader);
        return program;
    }
    private void visit(String fileId, HtAst.Unit unit, SourceLoader loader) {
        if (!units.containsKey(fileId)) {
            units.put(fileId, unit);
            collect(unit);
        }
        if (!visiting.add(fileId)) {
            throw new HtException(fileId, 0, 0, "import 出现循环引用");
        }
        for (HtAst.ImportStmt imp : unit.imports()) {
            String target = normalize(fileId, imp.path());
            if (units.containsKey(target)) {
                continue;
            }
            try {
                String content = loader.load(target);
                if (content == null) {
                    errors.add(fileId + ":" + imp.line()
                            + ": import 的文件不存在，已跳过 → " + target);
                    continue;
                }
                HtAst.Unit child = HtParser.parse(target, content);
                visit(target, child, loader);
            } catch (IOException e) {
                errors.add(fileId + ":" + imp.line() + ": import 读取失败，已跳过 → "
                        + target + " (" + e.getMessage() + ")");
            } catch (HtException e) {
                // 单个文件写错只影响它自己，不应让整张注册表失效
                errors.add("已跳过 " + target + "：" + e.getMessage());
            } catch (RuntimeException e) {
                errors.add("已跳过 " + target + "：" + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        }
        visiting.remove(fileId);
    }
    public static String normalize(String currentFile, String path) {
        String result = path.trim();
        String namespace;
        int colon = result.indexOf(':');
        if (colon >= 0) {
            namespace = result.substring(0, colon);
            result = result.substring(colon + 1);
        } else {
            int c = currentFile.indexOf(':');
            namespace = c >= 0 ? currentFile.substring(0, c) : "minecraft";
        }
        if (!result.endsWith(".ht")) {
            result = result + ".ht";
        }
        while (result.contains("//")) {
            result = result.replace("//", "/");
        }
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        return namespace + ":" + result;
    }
    private void collect(HtAst.Unit unit) {
        String fileId = unit.fileId();
        for (HtAst.Stmt stmt : unit.fileVars()) {
            collectStmt(stmt, fileId);
        }
        HtAst.Decl main = unit.main();
        if (main instanceof HtAst.LibraryDecl lib) {
            for (HtAst.Stmt stmt : lib.body()) {
                collectStmt(stmt, fileId);
            }
        } else if (main instanceof HtAst.ScreenDecl screen) {
            if (screens.containsKey(screen.name())) {
                throw new HtException(fileId, screen.line(), 1,
                        "screen 重名：" + screen.name() + "（已在 "
                                + screens.get(screen.name()).fileId() + " 中定义）");
            }
            screens.put(screen.name(), new ScreenEntry(screen, fileId));
        } else if (main instanceof HtAst.WidgetDecl widget) {
            registerWidget(widget, fileId);
        } else if (main instanceof HtAst.RegisterDef reg) {
            for (Map.Entry<String, String> e : reg.ids().entrySet()) {
                String prev = register.put(e.getKey(), e.getValue());
                if (prev != null && !prev.equals(e.getValue())) {
                    throw new HtException(fileId, reg.line(), 1,
                            "screen id 重复注册：" + e.getKey() + "（" + prev + " / " + e.getValue() + "）");
                }
            }
        }
    }
    private void collectStmt(HtAst.Stmt stmt, String fileId) {
        if (stmt instanceof HtAst.WidgetDefStmt w) {
            registerWidget(w.decl(), fileId);
        } else if (stmt instanceof HtAst.MethodDecl m) {
            HtAst.MethodDecl prev = methods.put(m.name(), m);
            if (prev != null) {
                throw new HtException(fileId, m.line(), 1,
                        "方法重名：" + m.name() + "（HT 不支持重载）");
            }
        } else if (stmt instanceof HtAst.VarDecl) {
            globalVars.add(stmt);
        }
    }
    private void registerWidget(HtAst.WidgetDecl widget, String fileId) {
        HtAst.WidgetDecl prev = widgets.put(widget.name(), widget);
        if (prev != null) {
            throw new HtException(fileId, widget.line(), 1,
                    "widget 模板重名：" + widget.name() + "（已在 "
                            + widgetOwner.get(widget.name()) + " 中定义）");
        }
        widgetOwner.put(widget.name(), fileId);
    }
    /** import 展开过程中被跳过的坏文件（供上层打日志）。 */
    public List<String> errors() {
        return errors;
    }

    public String rootFile() {
        return rootFile;
    }
    public HtAst.Unit rootUnit() {
        return rootUnit;
    }
    public Map<String, HtAst.WidgetDecl> widgets() {
        return widgets;
    }
    public HtAst.WidgetDecl widget(String name) {
        return widgets.get(name);
    }
    public Map<String, HtAst.MethodDecl> methods() {
        return methods;
    }
    public List<HtAst.Stmt> globalVars() {
        return globalVars;
    }
    public Map<String, ScreenEntry> screens() {
        return screens;
    }
    public LinkedHashMap<String, String> register() {
        return register;
    }
    public HtAst.ScreenDecl rootScreen() {
        return rootUnit.main() instanceof HtAst.ScreenDecl s ? s : null;
    }
    public List<HtAst.ScreenDecl> screenChain(HtAst.ScreenDecl screen) {
        List<HtAst.ScreenDecl> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        HtAst.ScreenDecl cur = screen;
        while (cur != null) {
            if (!seen.add(cur.name())) {
                throw new HtException(rootFile, cur.line(), 1,
                        "screen extends 出现循环：" + cur.name());
            }
            chain.add(0, cur);
            if (cur.parent() == null) {
                break;
            }
            ScreenEntry parentEntry = screens.get(cur.parent());
            if (parentEntry == null) {
                throw new HtException(rootFile, cur.line(), 1,
                        "extends 找不到父 screen：" + cur.parent());
            }
            cur = parentEntry.decl();
        }
        return chain;
    }
    public List<HtAst.WidgetDecl> widgetChain(HtAst.WidgetDecl widget) {
        List<HtAst.WidgetDecl> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        HtAst.WidgetDecl cur = widget;
        while (cur != null) {
            if (!seen.add(cur.name())) {
                throw new HtException(rootFile, cur.line(), 1,
                        "widget extends 出现循环：" + cur.name());
            }
            chain.add(0, cur);
            if (cur.parent() == null) {
                break;
            }
            HtAst.WidgetDecl parent = widgets.get(cur.parent());
            if (parent == null) {
                throw new HtException(rootFile, cur.line(), 1,
                        "extends 找不到父模板：" + cur.parent());
            }
            if (!parent.type().equals(cur.type())) {
                throw new HtException(rootFile, cur.line(), 1,
                        "extends 类型不匹配：" + cur.name() + " 是 " + cur.type()
                                + "，父模板 " + parent.name() + " 是 " + parent.type());
            }
            cur = parent;
        }
        return chain;
    }
}
