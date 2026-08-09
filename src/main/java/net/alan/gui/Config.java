package net.alan.gui;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_UI = BUILDER
            .comment("Enable SIRIUS Ui's Json Ui Engine. Set to false to use vanilla screens.")
            .define("enableCustomUi", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}