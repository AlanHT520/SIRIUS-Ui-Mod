package net.alan.gui.data.props;

public class DynamicListStyleConfig {
    private RowStyleConfig row_style;
    private ScrollbarConfig scrollbar;
    private DividerConfig divider;

    public RowStyleConfig rowStyle() { return row_style; }
    public ScrollbarConfig scrollbar() { return scrollbar; }
    public DividerConfig divider() { return divider; }

    public static class RowStyleConfig {
        public String background_color = "0x10000000";
        public String background_color_alt = "0x00000000";
        public String hover_background_color = "0x20FFFFFF";
        public String background_texture = null;
        public String text_color = "0xFFFFFFFF";
        public String text_color_alt = "0xFFAAAAAA";
        public boolean shadow = true;
    }

    public static class ScrollbarConfig {
        public String track_color = "0x33000000";
        public String thumb_color = "0xAAFFFFFF";
        public int width = 4;
    }

    public static class DividerConfig {
        public int height = 0;
        public String color = "0x00000000";
    }
}