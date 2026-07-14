package net.alan.gui.data.source;

import net.alan.gui.data.DynamicListData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WorldSaveDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSaveDataSource.class);

    public static List<DynamicListData> load() {
        List<DynamicListData> list = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            var levelSource = mc.getLevelSource();
            var candidates = levelSource.findLevelCandidates();
            List<LevelSummary> saves = levelSource.loadLevelSummaries(candidates).join();
            for (LevelSummary s : saves) {
                String iconPath = s.getIcon() != null ? s.getIcon().toString() : null;
                list.add(new DynamicListData(
                        s.getLevelId(),
                        s.getLevelName(),
                        s.getGameMode().getName(),
                        iconPath,
                        "join_world",
                        !s.isLocked() && !s.isDisabled()
                ));
            }
            list.sort(Comparator.comparing(
                    d -> d.getName().toLowerCase(),
                    String.CASE_INSENSITIVE_ORDER
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to load world saves", e);
        }
        return list;
    }
}