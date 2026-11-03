

package net.alan.gui.ht;
public class HtException extends RuntimeException {
    private final String file;
    private final int line;
    private final int col;
    public HtException(String message) {
        this("?", 0, 0, message, null);
    }
    public HtException(String file, int line, int col, String message) {
        this(file, line, col, message, null);
    }
    public HtException(String file, int line, int col, String message, Throwable cause) {
        super(build(file, line, col, message), cause);
        this.file = file;
        this.line = line;
        this.col = col;
    }
    public String file() {
        return file;
    }
    public int line() {
        return line;
    }
    public int col() {
        return col;
    }
    public static HtException at(String file, int line, int col, String message) {
        return new HtException(file, line, col, message);
    }
    public static HtException at(String file, HtToken token, String message) {
        return new HtException(file, token.line(), token.col(), message);
    }
    private static String build(String file, int line, int col, String message) {
        if (file == null) {
            return "[ht] " + message;
        }
        if (line <= 0) {
            return "[ht] " + file + ": " + message;
        }
        return "[ht] " + file + ":" + line + ":" + col + ": " + message;
    }
}
