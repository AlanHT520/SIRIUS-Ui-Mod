

package net.alan.gui.ht;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import static net.alan.gui.ht.HtToken.Kind.*;
public final class HtParser {
    public static final Set<String> WIDGET_TYPES = Set.of(
            "container", "text", "image", "button", "button_content", "slider",
            "arrow_switch", "cycle_button", "selector", "dropdown", "edit_box",
            "input_field", "list", "dynamic_list", "pack_list", "content", "box",
            "book", "entity_display", "splash", "card"
    );
    private static final Set<String> TYPE_NAMES = Set.of(
            "int", "float", "string", "bool", "color", "void"
    );
    private static final Set<String> PROPS_BLOCKS = Set.of(
            "texture", "textures", "variables", "member", "search", "layout",
            "divider", "scrollbar", "row_template", "row_style", "filled", "handle",
            "left_arrow", "right_arrow", "pages", "segments", "rows", "title_bar",
            "pulse", "splashes", "date_splashes", "easter_egg", "active_row",
            "unactive_row", "shadow", "animation", "attack", "track", "thumb",
            "background", "row", "default", "card", "style", "content", "header",
            "footer", "items", "columns", "groups", "slides", "play", "tooltip",
            "text", "size", "position", "values"
    );
    private final String file;
    private final List<HtToken> tokens;
    private final List<HtAst.ImportStmt> imports = new ArrayList<>();
    private int current;
    public HtParser(String file, List<HtToken> tokens) {
        this.file = file;
        this.tokens = tokens;
    }
    public static HtAst.Unit parse(String file, String source) {
        return new HtParser(file, HtLexer.tokenize(file, source)).parseUnit();
    }
    public HtAst.Unit parseUnit() {
        List<HtAst.Stmt> fileVars = new ArrayList<>();
        HtAst.Decl main = null;
        while (!check(EOF)) {
            if (check(KW_IMPORT)) {
                parseImport();
                continue;
            }
            if (main != null) {
                throw error(peek(), "一个文件只能有一个主声明（library / screen / widget / register），"
                        + "但这里在主声明之后又出现了 " + peek().describe());
            }
            switch (peek().kind()) {
                case KW_LIBRARY -> main = parseLibrary();
                case KW_SCREEN -> main = parseScreen();
                case KW_WIDGET -> main = parseWidgetDecl();
                case KW_REGISTER -> main = parseRegister();
                default -> {
                    HtAst.Stmt varOrMethod = parseVarOrMethod();
                    if (varOrMethod == null) {
                        throw error(peek(), "顶层只允许 import、变量声明、方法定义或主声明，实际遇到 "
                                + peek().describe());
                    }
                    fileVars.add(varOrMethod);
                }
            }
        }
        if (main == null) {
            throw error(peek(), "文件缺少主声明（library / screen / widget / register）");
        }
        return HtAst.Unit.of(file, imports, fileVars, main);
    }
    private HtAst.ImportStmt parseImport() {
        int line = peek().line();
        expect(KW_IMPORT, "import");
        HtToken pathTok = expect(STRING, "import 路径字符串");
        String alias = null;
        if (match(KW_AS)) {
            alias = expectIdent("别名").lexeme();
        }
        optionalSemi();
        HtAst.ImportStmt stmt = new HtAst.ImportStmt((String) pathTok.literal(), alias, line);
        imports.add(stmt);
        return stmt;
    }
    private HtAst.LibraryDecl parseLibrary() {
        int line = peek().line();
        expect(KW_LIBRARY, "library");
        String name = expectIdent("library 名").lexeme();
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.LibraryDecl(name, body, line);
    }
    private HtAst.ScreenDecl parseScreen() {
        int line = peek().line();
        expect(KW_SCREEN, "screen");
        String name = expectIdent("screen 名").lexeme();
        String parent = null;
        if (match(KW_EXTENDS)) {
            parent = expectIdent("父 screen 名").lexeme();
        }
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.ScreenDecl(name, parent, body, line);
    }
    private HtAst.WidgetDecl parseWidgetDecl() {
        int line = peek().line();
        expect(KW_WIDGET, "widget");
        String type = expectIdent("控件类型").lexeme();
        String name = expectIdent("模板名").lexeme();
        String parent = null;
        if (match(KW_EXTENDS)) {
            parent = expectIdent("父模板名").lexeme();
        }
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.WidgetDecl(type, name, parent, body, line);
    }
    private HtAst.RegisterDef parseRegister() {
        int line = peek().line();
        expect(KW_REGISTER, "register");
        expect(LBRACE, "{");
        LinkedHashMap<String, String> ids = new LinkedHashMap<>();
        while (!check(RBRACE) && !check(EOF)) {
            match(KW_SCREEN_ID);
            String id = expectIdent("screen id").lexeme();
            expect(ASSIGN, "=");
            String target = expectIdent("screen 名").lexeme();
            optionalSemi();
            ids.put(id, target);
        }
        expect(RBRACE, "}");
        return new HtAst.RegisterDef(ids, line);
    }
    private List<HtAst.Stmt> parseBraceBody() {
        expect(LBRACE, "{");
        List<HtAst.Stmt> body = new ArrayList<>();
        while (!check(RBRACE) && !check(EOF)) {
            if (check(KW_IMPORT)) {
                parseImport();
                continue;
            }
            HtAst.Stmt stmt = parseStmt();
            if (stmt != null) {
                body.add(stmt);
            }
        }
        expect(RBRACE, "}");
        return body;
    }
    private HtAst.Stmt parseStmt() {
        HtToken t = peek();
        if (isVarDeclStart()) {
            HtAst.Stmt vm = parseVarOrMethod();
            if (vm == null) {
                throw error(t, "无法解析的声明：" + t.describe());
            }
            return vm;
        }
        return switch (t.kind()) {
            case KW_RETURN -> parseReturn();
            case KW_IF -> parseIf();
            case KW_FOR -> parseFor();
            case LBRACE -> {
                List<HtAst.Stmt> body = parseBraceBody();
                yield new HtAst.Block(body, t.line());
            }
            case KW_WIDGET -> new HtAst.WidgetDefStmt(parseWidgetDecl(), t.line());
            case KW_FINAL -> {
                HtAst.Stmt vm = parseVarOrMethod();
                if (vm == null) {
                    throw error(t, "无法解析的声明：" + t.describe());
                }
                yield vm;
            }
            default -> parseSimpleStmt();
        };
    }
    private boolean isVarDeclStart() {
        return check(IDENT) && TYPE_NAMES.contains(peek().lexeme())
                && peek(1).kind() == IDENT;
    }
    private HtAst.Stmt parseReturn() {
        int line = peek().line();
        expect(KW_RETURN, "return");
        HtAst.Expr value = check(SEMI) ? null : parseExpr();
        optionalSemi();
        return new HtAst.Return(value, line);
    }
    private HtAst.Stmt parseIf() {
        int line = peek().line();
        expect(KW_IF, "if");
        expect(LPAREN, "(");
        HtAst.Expr cond = parseExpr();
        expect(RPAREN, ")");
        List<HtAst.Stmt> thenBody = parseBraceBody();
        List<HtAst.Stmt> elseBody = List.of();
        if (match(KW_ELSE)) {
            if (check(KW_IF)) {
                elseBody = List.of(parseIf());
            } else {
                elseBody = parseBraceBody();
            }
        }
        return new HtAst.If(cond, thenBody, elseBody, line);
    }
    private HtAst.Stmt parseFor() {
        int line = peek().line();
        expect(KW_FOR, "for");
        expect(LPAREN, "(");
        String first = expectIdent("循环变量名").lexeme();
        String second = null;
        if (match(COMMA)) {
            second = expectIdent("循环变量名").lexeme();
        }
        expect(KW_IN, "in");
        HtAst.Expr source = parseExpr();
        expect(RPAREN, ")");
        String itemName = first;
        String indexName = second;
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.For(itemName, indexName, source, body, line);
    }
    private HtAst.Stmt parseVarOrMethod() {
        int line = peek().line();
        boolean constant = match(KW_FINAL);
        String type = null;
        if (check(IDENT) && TYPE_NAMES.contains(peek().lexeme()) && peek(1).kind() == IDENT) {
            type = advance().lexeme();
        } else if (!constant) {
            return null;
        }
        if (!check(IDENT)) {
            if (constant) {
                throw error(peek(), "final 之后需要「类型 变量名」或「变量名」");
            }
            return null;
        }
        String name = advance().lexeme();
        if (check(LPAREN)) {
            List<HtAst.Param> params = parseParams();
            List<HtAst.Stmt> body = parseBraceBody();
            return new HtAst.MethodDecl(type, name, params, body, line);
        }
        HtAst.Expr init = null;
        if (match(ASSIGN)) {
            init = parseExpr();
        }
        optionalSemi();
        return new HtAst.VarDecl(constant, type, name, init, line);
    }
    private List<HtAst.Param> parseParams() {
        expect(LPAREN, "(");
        List<HtAst.Param> params = new ArrayList<>();
        while (!check(RPAREN) && !check(EOF)) {
            String type = null;
            if (check(IDENT) && TYPE_NAMES.contains(peek().lexeme())
                    && peek(1).kind() == IDENT) {
                type = advance().lexeme();
            }
            String name = expectIdent("参数名").lexeme();
            params.add(new HtAst.Param(type, name));
            if (!match(COMMA)) {
                break;
            }
        }
        expect(RPAREN, ")");
        return params;
    }
    private HtAst.Stmt parseSimpleStmt() {
        HtToken t = peek();
        if (t.isIdent()) {
            HtToken next = peek(1);
            String word = t.lexeme();
            if (next.kind() == DOT && peek(2).isIdent() && "on".equals(peek(2).lexeme())
                    && peek(3).isIdent() && peek(4).kind() == LBRACE) {
                return parseInstanceEvent();
            }
            if (next.kind() == DOT && peek(2).isIdent() && peek(3).kind() == LBRACE) {
                return parseInstanceProps();
            }
            switch (word) {
                case "state" -> {
                    if (next.kind() == LPAREN) {
                        return parseStateBlock();
                    }
                }
                case "sound" -> {
                    if (next.kind() == LBRACE) {
                        return parseSoundBlock();
                    }
                }
                case "animate" -> {
                    if (next.kind() == LBRACE) {
                        return parseAnimateBlock();
                    }
                }
                case "shared_state" -> {
                    if (next.kind() == LBRACE) {
                        return new HtAst.SharedState(parseBraceBodyAfterAdvance(), t.line());
                    }
                }
                case "panorama" -> {
                    if (next.kind() == LBRACE) {
                        return new HtAst.Panorama(parseBraceBodyAfterAdvance(), t.line());
                    }
                }
                case "background" -> {
                    if (next.kind() == LPAREN) {
                        return parseBackground();
                    }
                }
                case "graphicsDraw" -> {
                    if (next.kind() == LPAREN) {
                        return parseGraphicsDraw();
                    }
                }
                case "on" -> {
                    if (next.kind() == IDENT) {
                        return parseEvent();
                    }
                }
                default -> {
                    if (PROPS_BLOCKS.contains(word) && next.kind() == LBRACE) {
                        advance();
                        return new HtAst.PropsBlock(word, parseBraceBody(), t.line());
                    }
                }
            }
            if (next.isIdent() && WIDGET_TYPES.contains(word)) {
                return parseInstance();
            }
            if ((word.equals("page") || word.equals("segment") || word.equals("row")
                    || word.equals("group"))
                    && next.kind() == IDENT && peek(2).kind() == LBRACE) {
                advance();
                String id = advance().lexeme();
                return new HtAst.PropsBlock(word + ":" + id, parseBraceBody(), t.line());
            }
        }
        if (check(STRING) && peek(1).kind() == ASSIGN) {
            HtToken keyToken = advance();
            advance();
            HtAst.Expr value = parseExpr();
            optionalSemi();
            return new HtAst.Assign(
                    new HtAst.Name((String) keyToken.literal(), keyToken.line()),
                    value, keyToken.line());
        }
        HtAst.Expr target = parseExpr();
        if (match(ASSIGN)) {
            HtAst.Expr value = parseExpr();
            optionalSemi();
            return new HtAst.Assign(target, value, t.line());
        }
        optionalSemi();
        return new HtAst.ExprStmt(target, t.line());
    }
    private List<HtAst.Stmt> parseBraceBodyAfterAdvance() {
        advance();
        return parseBraceBody();
    }
    private HtAst.Stmt parseInstanceEvent() {
        int line = peek().line();
        String instance = advance().lexeme();
        expect(DOT, ".");
        advance();
        String event = expectIdent("事件名（clicked / hover / ...）").lexeme();
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.InstanceEvent(instance, event, body, line);
    }
    private HtAst.Stmt parseInstanceProps() {
        int line = peek().line();
        String instance = advance().lexeme();
        expect(DOT, ".");
        String key = expectIdent("属性块名").lexeme();
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.InstanceProps(instance, key, body, line);
    }
    private HtAst.Stmt parseInstance() {
        int line = peek().line();
        String type = advance().lexeme();
        String name = expectIdent("实例名").lexeme();
        String template = null;
        if (match(ASSIGN)) {
            template = expectIdent("模板名").lexeme();
        }
        List<HtAst.Stmt> body = List.of();
        if (check(LBRACE)) {
            body = parseBraceBody();
        }
        optionalSemi();
        return new HtAst.Instance(type, name, template, body, line);
    }
    private HtAst.Stmt parseStateBlock() {
        int line = peek().line();
        advance();
        expect(LPAREN, "(");
        List<String> states = new ArrayList<>();
        while (!check(RPAREN) && !check(EOF)) {
            states.add(expectIdent("状态名").lexeme());
            if (!match(COMMA)) {
                break;
            }
        }
        expect(RPAREN, ")");
        List<HtAst.Stmt> body = parseBraceBody();
        if (states.isEmpty()) {
            throw new HtException(file, line, 1, "state(...) 至少需要一个状态名");
        }
        return new HtAst.StateBlock(states, body, line);
    }
    private HtAst.Stmt parseSoundBlock() {
        int line = peek().line();
        advance();
        expect(LBRACE, "{");
        List<HtAst.Stmt> entries = new ArrayList<>();
        while (!check(RBRACE) && !check(EOF)) {
            int entryLine = peek().line();
            String event = expectIdent("音效事件名（click / hover / ...）").lexeme();
            HtValue.Str soundId = null;
            List<HtAst.Stmt> config;
            if (match(ASSIGN)) {
                HtToken idTok = expect(STRING, "音效 id 字符串");
                soundId = HtValue.str((String) idTok.literal());
                config = check(LBRACE) ? parseBraceBody() : List.of();
            } else {
                // 块写法：value_change { id = "slider_value_change"; throttle_ticks = 2; }
                config = parseBraceBody();
            }
            optionalSemi();
            entries.add(new HtAst.SoundEntry(event, soundId, config, entryLine));
        }
        expect(RBRACE, "}");
        return new HtAst.SoundBlock(entries, line);
    }
    private HtAst.Stmt parseAnimateBlock() {
        int line = peek().line();
        advance();
        expect(LBRACE, "{");
        List<HtAst.Stmt> entries = new ArrayList<>();
        while (!check(RBRACE) && !check(EOF)) {
            int entryLine = peek().line();
            String type = expectIdent("动画类型（typewriter / hold / ...）").lexeme();
            List<HtAst.Arg> args = parseArgs();
            optionalSemi();
            entries.add(new HtAst.AnimateEntry(type, args, entryLine));
        }
        expect(RBRACE, "}");
        return new HtAst.AnimateBlock(entries, line);
    }
    private HtAst.Stmt parseEvent() {
        int line = peek().line();
        advance();
        String name = expectIdent("事件名（clicked / hover / submit / ...）").lexeme();
        List<HtAst.Param> params = List.of();
        if (check(LPAREN)) {
            params = parseParams();
        }
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.Event(name, params, body, line);
    }
    private HtAst.Stmt parseBackground() {
        int line = peek().line();
        advance();
        expect(LPAREN, "(");
        String type = expectIdent("背景类型（image / video / vanilla / color / slideshow）").lexeme();
        expect(RPAREN, ")");
        List<HtAst.Stmt> body = parseBraceBody();
        return new HtAst.Background(type, body, line);
    }
    private HtAst.Stmt parseGraphicsDraw() {
        int line = peek().line();
        advance();
        expect(LPAREN, "(");
        String kind = expectIdent("绘制类型（rect / image）").lexeme();
        List<HtAst.Arg> args = new ArrayList<>();
        if (match(COMMA)) {
            while (!check(RPAREN) && !check(EOF)) {
                args.add(new HtAst.Arg(null, parseExpr()));
                if (!match(COMMA)) {
                    break;
                }
            }
        }
        expect(RPAREN, ")");
        optionalSemi();
        return new HtAst.GraphicsDraw(kind, args, line);
    }
    public HtAst.Expr parseExpr() {
        return parseTernary();
    }
    private HtAst.Expr parseTernary() {
        HtAst.Expr cond = parseOr();
        if (match(QUESTION)) {
            int line = previous().line();
            HtAst.Expr a = parseExpr();
            expect(COLON, "三元表达式的 :");
            HtAst.Expr b = parseTernary();
            return new HtAst.Ternary(cond, a, b, line);
        }
        return cond;
    }
    private HtAst.Expr parseOr() {
        HtAst.Expr left = parseAnd();
        while (match(OROR)) {
            int line = previous().line();
            left = new HtAst.Binary("||", left, parseAnd(), line);
        }
        return left;
    }
    private HtAst.Expr parseAnd() {
        HtAst.Expr left = parseBitOr();
        while (match(ANDAND)) {
            int line = previous().line();
            left = new HtAst.Binary("&&", left, parseBitOr(), line);
        }
        return left;
    }
    private HtAst.Expr parseBitOr() {
        HtAst.Expr left = parseBitAnd();
        while (match(PIPE)) {
            int line = previous().line();
            left = new HtAst.Binary("|", left, parseBitAnd(), line);
        }
        return left;
    }
    private HtAst.Expr parseBitAnd() {
        HtAst.Expr left = parseEquality();
        while (match(AMP)) {
            int line = previous().line();
            left = new HtAst.Binary("&", left, parseEquality(), line);
        }
        return left;
    }
    private HtAst.Expr parseEquality() {
        HtAst.Expr left = parseComparison();
        while (true) {
            if (match(EQEQ)) {
                left = new HtAst.Binary("==", left, parseComparison(), previous().line());
            } else if (match(NEQ)) {
                left = new HtAst.Binary("!=", left, parseComparison(), previous().line());
            } else {
                return left;
            }
        }
    }
    private HtAst.Expr parseComparison() {
        HtAst.Expr left = parseAdditive();
        while (true) {
            if (match(GTE)) {
                left = new HtAst.Binary(">=", left, parseAdditive(), previous().line());
            } else if (match(LTE)) {
                left = new HtAst.Binary("<=", left, parseAdditive(), previous().line());
            } else if (match(GT)) {
                left = new HtAst.Binary(">", left, parseAdditive(), previous().line());
            } else if (match(LT)) {
                left = new HtAst.Binary("<", left, parseAdditive(), previous().line());
            } else {
                return left;
            }
        }
    }
    private HtAst.Expr parseAdditive() {
        HtAst.Expr left = parseMultiplicative();
        while (true) {
            if (match(PLUS)) {
                left = new HtAst.Binary("+", left, parseMultiplicative(), previous().line());
            } else if (match(MINUS)) {
                left = new HtAst.Binary("-", left, parseMultiplicative(), previous().line());
            } else {
                return left;
            }
        }
    }
    private HtAst.Expr parseMultiplicative() {
        HtAst.Expr left = parseUnary();
        while (true) {
            if (match(STAR)) {
                left = new HtAst.Binary("*", left, parseUnary(), previous().line());
            } else if (match(SLASH)) {
                left = new HtAst.Binary("/", left, parseUnary(), previous().line());
            } else if (match(PERCENT)) {
                left = new HtAst.Binary("%", left, parseUnary(), previous().line());
            } else {
                return left;
            }
        }
    }
    private HtAst.Expr parseUnary() {
        if (match(NOT)) {
            int line = previous().line();
            return new HtAst.Unary("!", parseUnary(), line);
        }
        if (match(MINUS)) {
            int line = previous().line();
            return new HtAst.Unary("-", parseUnary(), line);
        }
        return parsePostfix();
    }
    private HtAst.Expr parsePostfix() {
        HtAst.Expr expr = parsePrimary();
        while (true) {
            if (match(DOT)) {
                int line = previous().line();
                String name = expectIdent("属性名").lexeme();
                if (check(LPAREN)) {
                    List<HtAst.Arg> args = parseArgs();
                    expr = new HtAst.Call(expr, name, args, line);
                } else {
                    expr = new HtAst.Member(expr, name, line);
                }
            } else if (check(LPAREN)) {
                int line = peek().line();
                String name = expr instanceof HtAst.Name n ? n.name() : null;
                if (name == null) {
                    throw error(peek(), "只有具名函数可以被直接调用");
                }
                List<HtAst.Arg> args = parseArgs();
                expr = new HtAst.Call(null, name, args, line);
            } else {
                return expr;
            }
        }
    }
    private HtAst.Expr parsePrimary() {
        HtToken t = peek();
        switch (t.kind()) {
            case INT -> {
                advance();
                return new HtAst.Lit(HtValue.num((Long) t.literal()), t.line());
            }
            case FLOAT -> {
                advance();
                return new HtAst.Lit(HtValue.real((Double) t.literal()), t.line());
            }
            case STRING -> {
                advance();
                return new HtAst.Lit(HtValue.str((String) t.literal()), t.line());
            }
            case COLOR -> {
                advance();
                return new HtAst.Lit(new HtValue.Color((Integer) t.literal()), t.line());
            }
            case KW_TRUE -> {
                advance();
                return new HtAst.Lit(HtValue.TRUE, t.line());
            }
            case KW_FALSE -> {
                advance();
                return new HtAst.Lit(HtValue.FALSE, t.line());
            }
            case KW_NULL -> {
                advance();
                return new HtAst.Lit(HtValue.NULL, t.line());
            }
            case KW_THIS -> {
                advance();
                return new HtAst.Name("this", t.line());
            }
            case KW_SCREEN -> {
                advance();
                return new HtAst.Name("screen", t.line());
            }
            case IDENT -> {
                advance();
                return new HtAst.Name(t.lexeme(), t.line());
            }
            case LPAREN -> {
                advance();
                HtAst.Expr inner = parseExpr();
                expect(RPAREN, ")");
                return inner;
            }
            case LBRACKET -> {
                advance();
                List<HtAst.Expr> items = new ArrayList<>();
                while (!check(RBRACKET) && !check(EOF)) {
                    items.add(parseExpr());
                    if (!match(COMMA)) {
                        break;
                    }
                }
                expect(RBRACKET, "]");
                return new HtAst.ArrLit(items, t.line());
            }
            case LBRACE -> {
                return parseObjectLiteral();
            }
            default -> throw error(t, "这里需要表达式，实际遇到 " + t.describe());
        }
    }
    private HtAst.Expr parseObjectLiteral() {
        HtToken start = peek();
        expect(LBRACE, "{");
        LinkedHashMap<String, HtAst.Expr> fields = new LinkedHashMap<>();
        while (!check(RBRACE) && !check(EOF)) {
            String name;
            if (check(STRING)) {
                name = (String) advance().literal();
            } else {
                name = expectIdent("字段名").lexeme();
            }
            if (!match(ASSIGN)) {
                expect(COLON, "=");
            }
            fields.put(name, parseExpr());
            // 字段分隔符：`,` 或 `;`（HT 里 `;` 才是语句分隔符，多行对象自然写分号）
            if (!match(COMMA) && !match(SEMI)) {
                break;
            }
        }
        expect(RBRACE, "}");
        return new HtAst.ObjLit(fields, start.line());
    }
    private List<HtAst.Arg> parseArgs() {
        expect(LPAREN, "(");
        List<HtAst.Arg> args = new ArrayList<>();
        while (!check(RPAREN) && !check(EOF)) {
            String argName = null;
            if (check(IDENT) && (peek(1).kind() == COLON || peek(1).kind() == ASSIGN)) {
                argName = advance().lexeme();
                advance();
            }
            args.add(new HtAst.Arg(argName, parseExpr()));
            if (!match(COMMA)) {
                break;
            }
        }
        expect(RPAREN, ")");
        return args;
    }
    private HtToken peek() {
        return tokens.get(Math.min(current, tokens.size() - 1));
    }
    private HtToken peek(int offset) {
        int idx = Math.min(current + offset, tokens.size() - 1);
        return tokens.get(idx);
    }
    private HtToken previous() {
        return tokens.get(Math.max(current - 1, 0));
    }
    private boolean check(HtToken.Kind kind) {
        return peek().kind() == kind;
    }
    private HtToken advance() {
        if (!check(EOF)) {
            current++;
        }
        return tokens.get(Math.max(current - 1, 0));
    }
    private boolean match(HtToken.Kind kind) {
        if (check(kind)) {
            advance();
            return true;
        }
        return false;
    }
    private HtToken expect(HtToken.Kind kind, String what) {
        if (!check(kind)) {
            throw error(peek(), "需要 " + what + "，实际遇到 " + peek().describe());
        }
        return advance();
    }
    private HtToken expectIdent(String what) {
        if (!check(IDENT)) {
            throw error(peek(), "需要" + what + "，实际遇到 " + peek().describe());
        }
        return advance();
    }
    private void optionalSemi() {
        match(SEMI);
    }
    private HtException error(HtToken token, String message) {
        return HtException.at(file, token, message);
    }
}
