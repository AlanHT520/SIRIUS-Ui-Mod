

package net.alan.gui.ht;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public final class HtLexer {
    private static final Map<String, HtToken.Kind> KEYWORDS = Map.ofEntries(
            Map.entry("import", HtToken.Kind.KW_IMPORT),
            Map.entry("as", HtToken.Kind.KW_AS),
            Map.entry("final", HtToken.Kind.KW_FINAL),
            Map.entry("extends", HtToken.Kind.KW_EXTENDS),
            Map.entry("if", HtToken.Kind.KW_IF),
            Map.entry("else", HtToken.Kind.KW_ELSE),
            Map.entry("for", HtToken.Kind.KW_FOR),
            Map.entry("in", HtToken.Kind.KW_IN),
            Map.entry("return", HtToken.Kind.KW_RETURN),
            Map.entry("true", HtToken.Kind.KW_TRUE),
            Map.entry("false", HtToken.Kind.KW_FALSE),
            Map.entry("null", HtToken.Kind.KW_NULL),
            Map.entry("this", HtToken.Kind.KW_THIS),
            Map.entry("library", HtToken.Kind.KW_LIBRARY),
            Map.entry("screen", HtToken.Kind.KW_SCREEN),
            Map.entry("widget", HtToken.Kind.KW_WIDGET),
            Map.entry("register", HtToken.Kind.KW_REGISTER),
            Map.entry("screen_id", HtToken.Kind.KW_SCREEN_ID)
    );
    private final String file;
    private final String src;
    private final int length;
    private final List<HtToken> tokens = new ArrayList<>();
    private int pos;
    private int line = 1;
    private int col = 1;
    public HtLexer(String file, String source) {
        this.file = file;
        this.src = source == null ? "" : source;
        this.length = this.src.length();
    }
    public static List<HtToken> tokenize(String file, String source) {
        return new HtLexer(file, source).scan();
    }
    public List<HtToken> scan() {
        if (length > 0 && src.charAt(0) == '\uFEFF') {
            pos = 1;
            col = 2;
        }
        while (!atEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                skipLineComment();
            } else if (c == '/' && peekNext() == '*') {
                skipBlockComment();
            } else if (c == '"') {
                scanString();
            } else if (isDigit(c)) {
                scanNumber();
            } else if (isIdentStart(c)) {
                scanIdent();
            } else {
                scanOperator();
            }
        }
        tokens.add(new HtToken(HtToken.Kind.EOF, "", null, line, col));
        return tokens;
    }
    private void skipLineComment() {
        while (!atEnd() && peek() != '\n') {
            advance();
        }
    }
    private void skipBlockComment() {
        int startLine = line;
        int startCol = col;
        advance();
        advance();
        while (!atEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        throw HtException.at(file, startLine, startCol, "块注释没有闭合（缺少 */）");
    }
    private void scanString() {
        int startLine = line;
        int startCol = col;
        advance();
        StringBuilder sb = new StringBuilder();
        while (!atEnd()) {
            char c = advance();
            if (c == '"') {
                add(HtToken.Kind.STRING, "\"" + sb + "\"", sb.toString(), startLine, startCol);
                return;
            }
            if (c == '\n') {
                throw HtException.at(file, startLine, startCol, "字符串没有闭合（缺少 \"）");
            }
            if (c == '\\') {
                if (atEnd()) {
                    break;
                }
                char e = advance();
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    case '\\' -> sb.append('\\');
                    case 'u' -> sb.append(scanUnicodeEscape(startLine, startCol));
                    default -> throw HtException.at(file, line, col,
                            "未知的转义字符 \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw HtException.at(file, startLine, startCol, "字符串没有闭合（缺少 \"）");
    }
    private char scanUnicodeEscape(int sLine, int sCol) {
        if (pos + 4 > length) {
            throw HtException.at(file, sLine, sCol, "\\u 转义不完整");
        }
        String hex = src.substring(pos, pos + 4);
        for (int i = pos; i < pos + 4; i++) {
            advance();
        }
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw HtException.at(file, sLine, sCol, "\\u 转义不是合法十六进制：" + hex);
        }
    }
    private void scanNumber() {
        int startLine = line;
        int startCol = col;
        int start = pos;
        if (peek() == '0' && (peekNext() == 'x' || peekNext() == 'X')) {
            advance();
            advance();
            int hexStart = pos;
            while (!atEnd() && isHex(peek())) {
                advance();
            }
            String hex = src.substring(hexStart, pos);
            if (hex.length() != 6 && hex.length() != 8) {
                throw HtException.at(file, startLine, startCol,
                        "颜色字面量必须是 6 位或 8 位十六进制（0xRRGGBB / 0xAARRGGBB），实际是 "
                                + hex.length() + " 位");
            }
            long v = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                v |= 0xFF000000L;
            }
            add(HtToken.Kind.COLOR, "0x" + hex, (int) v, startLine, startCol);
            return;
        }
        boolean isFloat = false;
        while (!atEnd() && isDigit(peek())) {
            advance();
        }
        if (!atEnd() && peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance();
            while (!atEnd() && isDigit(peek())) {
                advance();
            }
        }
        String text = src.substring(start, pos);
        if (isFloat) {
            add(HtToken.Kind.FLOAT, text, Double.parseDouble(text), startLine, startCol);
        } else {
            long v;
            try {
                v = Long.parseLong(text);
            } catch (NumberFormatException e) {
                throw HtException.at(file, startLine, startCol, "数字过大：" + text);
            }
            add(HtToken.Kind.INT, text, v, startLine, startCol);
        }
    }
    private void scanIdent() {
        int startLine = line;
        int startCol = col;
        int start = pos;
        while (!atEnd() && isIdentPart(peek())) {
            advance();
        }
        String word = src.substring(start, pos);
        HtToken.Kind kw = KEYWORDS.get(word);
        add(kw != null ? kw : HtToken.Kind.IDENT, word, word, startLine, startCol);
    }
    private void scanOperator() {
        int startLine = line;
        int startCol = col;
        char c = advance();
        switch (c) {
            case '(' -> add(HtToken.Kind.LPAREN, "(", null, startLine, startCol);
            case ')' -> add(HtToken.Kind.RPAREN, ")", null, startLine, startCol);
            case '{' -> add(HtToken.Kind.LBRACE, "{", null, startLine, startCol);
            case '}' -> add(HtToken.Kind.RBRACE, "}", null, startLine, startCol);
            case '[' -> add(HtToken.Kind.LBRACKET, "[", null, startLine, startCol);
            case ']' -> add(HtToken.Kind.RBRACKET, "]", null, startLine, startCol);
            case ',' -> add(HtToken.Kind.COMMA, ",", null, startLine, startCol);
            case ':' -> add(HtToken.Kind.COLON, ":", null, startLine, startCol);
            case ';' -> add(HtToken.Kind.SEMI, ";", null, startLine, startCol);
            case '.' -> add(HtToken.Kind.DOT, ".", null, startLine, startCol);
            case '?' -> add(HtToken.Kind.QUESTION, "?", null, startLine, startCol);
            case '+' -> add(HtToken.Kind.PLUS, "+", null, startLine, startCol);
            case '-' -> add(HtToken.Kind.MINUS, "-", null, startLine, startCol);
            case '*' -> add(HtToken.Kind.STAR, "*", null, startLine, startCol);
            case '/' -> add(HtToken.Kind.SLASH, "/", null, startLine, startCol);
            case '%' -> add(HtToken.Kind.PERCENT, "%", null, startLine, startCol);
            case '=' -> {
                if (match('=')) {
                    add(HtToken.Kind.EQEQ, "==", null, startLine, startCol);
                } else {
                    add(HtToken.Kind.ASSIGN, "=", null, startLine, startCol);
                }
            }
            case '!' -> {
                if (match('=')) {
                    add(HtToken.Kind.NEQ, "!=", null, startLine, startCol);
                } else {
                    add(HtToken.Kind.NOT, "!", null, startLine, startCol);
                }
            }
            case '<' -> {
                if (match('=')) {
                    add(HtToken.Kind.LTE, "<=", null, startLine, startCol);
                } else {
                    add(HtToken.Kind.LT, "<", null, startLine, startCol);
                }
            }
            case '>' -> {
                if (match('=')) {
                    add(HtToken.Kind.GTE, ">=", null, startLine, startCol);
                } else {
                    add(HtToken.Kind.GT, ">", null, startLine, startCol);
                }
            }
            case '&' -> {
                if (match('&')) {
                    add(HtToken.Kind.ANDAND, "&&", null, startLine, startCol);
                } else {
                    add(HtToken.Kind.AMP, "&", null, startLine, startCol);
                }
            }
            case '|' -> {
                if (match('|')) {
                    add(HtToken.Kind.OROR, "||", null, startLine, startCol);
                } else {
                    add(HtToken.Kind.PIPE, "|", null, startLine, startCol);
                }
            }
            case '\u3000' -> {
            }
            default -> throw HtException.at(file, startLine, startCol,
                    "无法识别的字符 '" + c + "'");
        }
    }
    private boolean atEnd() {
        return pos >= length;
    }
    private char peek() {
        return pos < length ? src.charAt(pos) : '\0';
    }
    private char peekNext() {
        return pos + 1 < length ? src.charAt(pos + 1) : '\0';
    }
    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }
    private boolean match(char expected) {
        if (atEnd() || src.charAt(pos) != expected) {
            return false;
        }
        advance();
        return true;
    }
    private void add(HtToken.Kind kind, String lexeme, Object literal, int l, int c) {
        tokens.add(new HtToken(kind, lexeme, literal, l, c));
    }
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    private static boolean isHex(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }
    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || isDigit(c);
    }
}
