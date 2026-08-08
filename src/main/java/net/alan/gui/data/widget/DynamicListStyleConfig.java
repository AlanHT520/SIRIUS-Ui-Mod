package net.alan.gui.data.widget;

public class DynamicListStyleConfig {
    private RowStyleConfig row_style;
    private ScrollbarConfig scrollbar;
    private DividerConfig divider;
    private int scroll_speed = 36;
    private int scrollbar_min_height = 16;
    private int scrollbar_padding = 8;
    private int search_gap = 4;

    public RowStyleConfig rowStyle() { return row_style; }
    public ScrollbarConfig scrollbar() { return scrollbar; }
    public DividerConfig divider() { return divider; }
    public int scrollSpeed() { return scroll_speed > 0 ? scroll_speed : 36; }
    public int scrollbarMinHeight() { return scrollbar_min_height > 0 ? scrollbar_min_height : 16; }
    public int scrollbarPadding() { return scrollbar_padding > 0 ? scrollbar_padding : 8; }
    public int searchGap() { return search_gap > 0 ? search_gap : 4; }

    public static class RowStyleConfig {
        public String background_color = "0x10000000";
        public String background_color_alt = "0x00000000";
        public String hover_background_color = "0x20FFFFFF";
        public String selected_background_color = null;
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