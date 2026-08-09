package net.alan.gui.data.source;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.resource.ResourcePackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DataPackBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPackBridge.class);
    private static final String SOURCE_ID = "data_packs";

    private static Path tempDataPackDir;
    private static PackRepository tempDataPackRepository;

    public static void initialize() {
        cleanup();

        try {
            tempDataPackDir = Files.createTempDirectory("mcworld-");
        } catch (IOException e) {
            LOGGER.error("Failed to create temp data pack directory", e);
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        tempDataPackRepository = ServerPacksSource.createPackRepository(tempDataPackDir);
        tempDataPackRepository.reload();
        tempDataPackRepository.setSelected(List.of("vanilla"));

        PackDataSource.register(SOURCE_ID, tempDataPackRepository, repo -> {});

        LOGGER.info("DataPackBridge initialized at {}", tempDataPackDir);
    }

    public static void cleanup() {
        PackDataSource.unregister(SOURCE_ID);

        if (tempDataPackRepository != null) {
            tempDataPackRepository = null;
        }

        if (tempDataPackDir != null) {
            try {
                Path path = tempDataPackDir;
                tempDataPackDir = null;
                try (var files = Files.walk(path)) {
                    files.sorted(java.util.Comparator.reverseOrder())
                         .forEach(p -> {
                             try { Files.delete(p); } catch (IOException ignored) {}
                         });
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to clean up temp data pack directory", e);
            }
        }
    }

    public static WorldDataConfiguration getDataConfiguration() {
        if (tempDataPackRepository == null) {
            return WorldDataConfiguration.DEFAULT;
        }

        List<String> enabled = new ArrayList<>(tempDataPackRepository.getSelectedIds());
        List<String> disabled = new ArrayList<>(tempDataPackRepository.getAvailableIds());
        disabled.removeAll(enabled);

        return new WorldDataConfiguration(
                new net.minecraft.world.level.DataPackConfig(enabled, disabled),
                FeatureFlags.DEFAULT_FLAGS
        );
    }
}