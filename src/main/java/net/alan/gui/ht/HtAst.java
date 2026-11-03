

package net.alan.gui.ht;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
public final class HtAst {
    private HtAst() {
    }
    public sealed interface Expr
            permits Lit, Name, Member, Binary, Unary, Ternary, Call, ArrLit, ObjLit {
        int line();
    }
    public record Lit(HtValue value, int line) implements Expr {}
    public record Name(String name, int line) implements Expr {}
    public record Member(Expr target, String name, int line) implements Expr {}
    public record Binary(String op, Expr left, Expr right, int line) implements Expr {}
    public record Unary(String op, Expr operand, int line) implements Expr {}
    public record Ternary(Expr cond, Expr thenExpr, Expr elseExpr, int line) implements Expr {}
    public record Arg(String name, Expr value) {}
    public record Call(Expr target, String name, List<Arg> args, int line) implements Expr {}
    public record ArrLit(List<Expr> items, int line) implements Expr {}
    public record ObjLit(LinkedHashMap<String, Expr> fields, int line) implements Expr {}
    public sealed interface Stmt
            permits VarDecl, MethodDecl, WidgetDefStmt, Assign, ExprStmt, If, For, Block, Return,
            Event, InstanceEvent, InstanceProps, StateBlock, SoundBlock, SoundEntry,
            AnimateBlock, AnimateEntry,
            PropsBlock, Background, Panorama, GraphicsDraw, SharedState, Instance, ImportStmt {
        int line();
    }
    public record InstanceEvent(String instance, String event, List<Stmt> body, int line)
            implements Stmt {}
    public record InstanceProps(String instance, String key, List<Stmt> body, int line)
            implements Stmt {}
    public record WidgetDefStmt(WidgetDecl decl, int line) implements Stmt {}
    public record ImportStmt(String path, String alias, int line) implements Stmt {}
    public record VarDecl(boolean constant, String type, String name, Expr init, int line)
            implements Stmt {}
    public record Param(String type, String name) {}
    public record MethodDecl(String type, String name, List<Param> params, List<Stmt> body, int line)
            implements Stmt {}
    public record Assign(Expr target, Expr value, int line) implements Stmt {}
    public record ExprStmt(Expr expr, int line) implements Stmt {}
    public record If(Expr cond, List<Stmt> thenBody, List<Stmt> elseBody, int line) implements Stmt {}
    public record For(String itemName, String indexName, Expr source, List<Stmt> body, int line)
            implements Stmt {}
    public record Block(List<Stmt> body, int line) implements Stmt {}
    public record Return(Expr value, int line) implements Stmt {}
    public record Event(String name, List<Param> params, List<Stmt> body, int line) implements Stmt {}
    public record StateBlock(List<String> states, List<Stmt> body, int line) implements Stmt {}
    public record SoundBlock(List<Stmt> body, int line) implements Stmt {}
    public record SoundEntry(String event, HtValue.Str soundId, List<Stmt> config, int line)
            implements Stmt {}
    public record AnimateBlock(List<Stmt> body, int line) implements Stmt {}
    public record AnimateEntry(String type, List<Arg> args, int line) implements Stmt {}
    public record PropsBlock(String key, List<Stmt> body, int line) implements Stmt {}
    public record Background(String type, List<Stmt> body, int line) implements Stmt {}
    public record Panorama(List<Stmt> body, int line) implements Stmt {}
    public record GraphicsDraw(String kind, List<Arg> args, int line) implements Stmt {}
    public record SharedState(List<Stmt> body, int line) implements Stmt {}
    public record Instance(String type, String name, String template, List<Stmt> body, int line)
            implements Stmt {}
    public sealed interface Decl permits WidgetDecl, ScreenDecl, LibraryDecl, RegisterDef {
        int line();
    }
    public record WidgetDecl(String type, String name, String parent, List<Stmt> body, int line)
            implements Decl {}
    public record ScreenDecl(String name, String parent, List<Stmt> body, int line)
            implements Decl {}
    public record LibraryDecl(String name, List<Stmt> body, int line) implements Decl {}
    public record RegisterDef(LinkedHashMap<String, String> ids, int line) implements Decl {}
    public record Unit(String fileId, List<ImportStmt> imports, List<Stmt> fileVars, Decl main, int line) {
        public static Unit of(String fileId, List<ImportStmt> imports, List<Stmt> fileVars, Decl main) {
            return new Unit(fileId, List.copyOf(imports), List.copyOf(fileVars), main, 1);
        }
    }
    public static List<Stmt> copy(List<Stmt> body) {
        return body == null ? new ArrayList<>() : new ArrayList<>(body);
    }
}
