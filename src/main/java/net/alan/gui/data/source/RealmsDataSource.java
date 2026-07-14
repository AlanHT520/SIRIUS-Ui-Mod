package net.alan.gui.data.source;

import net.alan.gui.data.DynamicListData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RealmsDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmsDataSource.class);

    public static List<DynamicListData> load() {
        List<DynamicListData> list = new ArrayList<>();
        list.add(new DynamicListData(
                "realms_subscription",
                "Realms 订阅",
                "查看和管理你的 Realms 订阅",
                null,
                "open_screen",
                true
        ));
        return list;
    }
}