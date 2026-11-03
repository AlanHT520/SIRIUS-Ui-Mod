

package net.alan.gui.ht;
public record HtToken(Kind kind, String lexeme, Object literal, int line, int col) {
    public enum Kind {
        IDENT,
        INT,
        FLOAT,
        STRING,
        COLOR,
        KW_IMPORT,
        KW_AS,
        KW_FINAL,
        KW_EXTENDS,
        KW_IF,
        KW_ELSE,
        KW_FOR,
        KW_IN,
        KW_RETURN,
        KW_TRUE,
        KW_FALSE,
        KW_NULL,
        KW_THIS,
        KW_LIBRARY,
        KW_SCREEN,
        KW_WIDGET,
        KW_REGISTER,
        KW_SCREEN_ID,
        TYPE_INT,
        TYPE_FLOAT,
        TYPE_STRING,
        TYPE_BOOL,
        TYPE_COLOR,
        TYPE_VOID,
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
        COMMA, COLON, SEMI, DOT, QUESTION,
        ASSIGN, PLUS, MINUS, STAR, SLASH, PERCENT,
        EQEQ, NEQ, LT, GT, LTE, GTE, ANDAND, OROR, NOT, AMP, PIPE,
        EOF
    }
    public boolean isIdent() {
        return kind == Kind.IDENT;
    }
    public boolean isTypeKeyword() {
        return switch (kind) {
            case TYPE_INT, TYPE_FLOAT, TYPE_STRING, TYPE_BOOL, TYPE_COLOR, TYPE_VOID -> true;
            default -> false;
        };
    }
    public String describe() {
        return switch (kind) {
            case EOF -> "文件结尾";
            case IDENT -> "标识符 '" + lexeme + "'";
            case STRING -> "字符串 \"" + lexeme + "\"";
            default -> "'" + lexeme + "'";
        };
    }
    @Override
    public String toString() {
        return kind + "(" + lexeme + ")@" + line + ":" + col;
    }
}
