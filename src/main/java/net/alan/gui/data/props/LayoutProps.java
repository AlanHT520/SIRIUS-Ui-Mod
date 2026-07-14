package net.alan.gui.data.props;

public record LayoutProps(
        String xExpr,
        String yExpr,
        String widthExpr,
        String heightExpr,
        boolean enabled,
        boolean visible,
        String condition
) {
    public LayoutProps() { this("0", "0", "auto", "auto", true, true, null); }
    public LayoutProps(String xExpr, String yExpr, String widthExpr, String heightExpr, boolean enabled, boolean visible) {
        this(xExpr, yExpr, widthExpr, heightExpr, enabled, visible, null);
    }
}