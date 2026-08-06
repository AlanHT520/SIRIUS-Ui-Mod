package net.alan.gui.render.popup;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PopupTheme {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopupTheme.class);
    private static final ResourceLocation THEME_LOCATION =
            ResourceLocation.fromNamespaceAndPath("sirius_ui", "popups/popup_theme.json");
    private static final Gson GSON = new Gson();
    private static PopupTheme instance;

    private PopupThemeData popup;

    public static void load(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(THEME_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.warn("popup_theme.json not found, using defaults");
                instance = new PopupTheme();
                instance.popup = new PopupThemeData();
                return;
            }
            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                PopupTheme theme = GSON.fromJson(reader, PopupTheme.class);
                if (theme != null && theme.popup != null) {
                    instance = theme;
                    LOGGER.info("Loaded popup theme");
                } else {
                    LOGGER.warn("popup_theme.json parsed to null, using defaults");
                    instance = new PopupTheme();
                    instance.popup = new PopupThemeData();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load popup_theme.json", e);
            instance = new PopupTheme();
            instance.popup = new PopupThemeData();
        }
    }

    public static PopupThemeData get() {
        if (instance == null || instance.popup == null) {
            instance = new PopupTheme();
            instance.popup = new PopupThemeData();
        }
        return instance.popup;
    }

    public static class PopupThemeData {
        public DialogTheme dialog = new DialogTheme();
        public ToastTheme toast = new ToastTheme();
        public LoadingTheme loading = new LoadingTheme();

        public static class DialogTheme {
            public int width = 300;
            @SerializedName("min_height") public int minHeight = 120;
            public int padding = 16;
            @SerializedName("overlay_color") public String overlayColor = "0xCC1A1A1A";
            @SerializedName("box_color") public String boxColor = "0xE6282828";
            @SerializedName("border_color") public String borderColor = "0xFF555555";
            @SerializedName("title_color") public String titleColor = "0xFFFFFFFF";
            @SerializedName("message_color") public String messageColor = "0xFFCCCCCC";
            @SerializedName("title_bar_color") public String titleBarColor = "0xFF2D2D2D";
            @SerializedName("separator_color") public String separatorColor = "0xFF444444";
            @SerializedName("shadow_color") public String shadowColor = "0x80000000";
            @SerializedName("shadow_size") public int shadowSize = 4;
            public ButtonTheme button = new ButtonTheme();
            public InputTheme input = new InputTheme();

            public int parseColor(String hex) {
                if (hex == null) return 0xFFFFFFFF;
                hex = hex.replace("0x", "").replace("0X", "");
                return (int) Long.parseLong(hex, 16);
            }

            public static class ButtonTheme {
                public int width = 120;
                public int height = 20;
                public int gap = 10;
                @SerializedName("texture_normal") public String textureNormal = "sirius_ui:buttons/default_button";
                @SerializedName("texture_highlighted") public String textureHighlighted = "sirius_ui:buttons/default_button_highlighted";
                @SerializedName("text_color") public String textColor = "0xFFFFFFFF";
            }

            public static class InputTheme {
                public int height = 20;
                public int gap = 8;
                @SerializedName("label_gap") public int labelGap = 4;
                @SerializedName("background_color") public String backgroundColor = "0xFF111111";
                @SerializedName("border_color") public String borderColor = "0xFF777777";
                @SerializedName("focus_border_color") public String focusBorderColor = "0xFFFFFFFF";
                @SerializedName("text_color") public String textColor = "0xFFFFFFFF";
                @SerializedName("hint_color") public String hintColor = "0xFF666666";
                @SerializedName("label_color") public String labelColor = "0xFFAAAAAA";
            }
        }

        public static class ToastTheme {
            public int width = 250;
            public int height = 30;
            @SerializedName("background_color") public String backgroundColor = "0xCC222222";
            @SerializedName("border_color") public String borderColor = "0xFF555555";
            @SerializedName("text_color") public String textColor = "0xFFFFFFFF";

            public int parseColor(String hex) {
                if (hex == null) return 0xFFFFFFFF;
                hex = hex.replace("0x", "").replace("0X", "");
                return (int) Long.parseLong(hex, 16);
            }
        }

        public static class LoadingTheme {
            @SerializedName("background_color") public String backgroundColor = "0x99000000";
            @SerializedName("box_color") public String boxColor = "0xFF2B2B2B";
            @SerializedName("border_color") public String borderColor = "0xFF555555";
            @SerializedName("text_color") public String textColor = "0xFFFFFFFF";

            public int parseColor(String hex) {
                if (hex == null) return 0xFFFFFFFF;
                hex = hex.replace("0x", "").replace("0X", "");
                return (int) Long.parseLong(hex, 16);
            }
        }
    }
}