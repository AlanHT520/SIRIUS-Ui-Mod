package net.alan.gui.client;

import net.alan.gui.Main;
import net.alan.gui.MainForge;
import net.alan.gui.data.source.CardDataSourceRegistry;
import net.alan.gui.data.source.DeleteConfirmDataSource;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MainForgeClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        MainForge.LOGGER.info("HELLO FROM CLIENT SETUP");
        MainForge.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        CardDataSourceRegistry.register("delete_confirm_info", new DeleteConfirmDataSource());
    }
}