package net.alan.gui.data.source;

import net.alan.gui.data.DynamicListData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ServerListDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerListDataSource.class);

    public static List<DynamicListData> load() {
        List<DynamicListData> list = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            ServerList serverList = new ServerList(mc);
            serverList.load();
            for (int i = 0; i < serverList.size(); i++) {
                ServerData s = serverList.get(i);
                list.add(new DynamicListData(
                        String.valueOf(i),
                        s.name,
                        s.ip,
                        null,
                        "join_server",
                        true
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load server list", e);
        }
        return list;
    }
}