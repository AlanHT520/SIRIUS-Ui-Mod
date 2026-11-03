

package net.alan.gui;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
@Mod(Main.MOD_ID)
public class MainNeo {
    public static final Logger LOGGER = LogUtils.getLogger();
    public MainNeo(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("SIRIUS Ui Common Setup "+Main.MOD_ID+" by "+Main.AUTHOR);
    }
}
