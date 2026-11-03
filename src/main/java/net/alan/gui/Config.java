

package net.alan.gui;
import net.neoforged.neoforge.common.ModConfigSpec;
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_UI = BUILDER
            .comment("Enable SIRIUS Ui's HT UI engine. Set to false to use vanilla screens.")
            .define("sirius_ui.enableCustomUi", true);
    static final ModConfigSpec SPEC = BUILDER.build();
}
