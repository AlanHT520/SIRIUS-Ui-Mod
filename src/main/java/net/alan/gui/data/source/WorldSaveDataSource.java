package net.alan.gui.data.source;

import net.alan.gui.data.DynamicListData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class WorldSaveDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSaveDataSource.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    public static List<DynamicListData> load() {
        List<DynamicListData> list = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            var levelSource = mc.getLevelSource();
            var candidates = levelSource.findLevelCandidates();
            List<LevelSummary> saves = levelSource.loadLevelSummaries(candidates).join();
            for (LevelSummary s : saves) {
                String iconPath = s.getIcon() != null ? s.getIcon().toString() : null;

                String dateStr = "";
                long lastPlayed = s.getLastPlayed();
                if (lastPlayed != -1L) {
                    dateStr = DATE_FORMAT.format(Instant.ofEpochMilli(lastPlayed));
                }

                String description = s.getInfo() != null ? s.getInfo().getString() : "";
                if (!dateStr.isEmpty()) {
                    description = dateStr + "  " + description;
                }

                DynamicListData data = new DynamicListData.Builder(s.getLevelId(), s.getLevelName())
                        .description(description)
                        .iconPath(iconPath)
                        .actionType("join_world")
                        .joinable(!s.isDisabled())
                        .lastPlayed(lastPlayed)
                        .versionInfo(s.getInfo() != null ? s.getInfo().getString() : "")
                        .gameMode(s.getGameMode().getName())
                        .isLocked(s.isLocked())
                        .isCompatible(s.isCompatible())
                        .canEdit(s.isCompatible())
                        .canDelete(!s.isLocked())
                        .canRecreate(s.isCompatible())
                        .shouldBackup(false)
                        .requiresManualConversion(s.requiresManualConversion())
                        .isExperimental(s.isExperimental())
                        .isDisabled(s.isDisabled())
                        .isDowngrade(!s.isCompatible())
                        .build();

                list.add(data);
            }
            list.sort(Comparator.comparing(
                    DynamicListData::getLastPlayed,
                    Comparator.reverseOrder()
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to load world saves", e);
        }
        return list;
    }
}