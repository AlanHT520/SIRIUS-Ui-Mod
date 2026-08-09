package net.alan.gui.data.source;

import net.alan.gui.data.DynamicListData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
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
                String description = buildServerDescription(s);
                DynamicListData.Builder builder = new DynamicListData.Builder(String.valueOf(i), s.name)
                        .description(description)
                        .iconPath(null)
                        .actionType("join_server")
                        .joinable(true)
                        .canEdit(true)
                        .canDelete(true)
                        .canRecreate(false)
                        .isLan(false)
                        .ping(s.ping)
                        .isOnline(s.ping != -1L);

                if (s.motd != null) {
                    builder.motd(s.motd.getString());
                }
                if (s.playerList != null) {
                    builder.onlinePlayers(s.playerList.size());
                }

                list.add(builder.build());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load server list", e);
        }
        return list;
    }

    private static String buildServerDescription(ServerData data) {
        StringBuilder sb = new StringBuilder();
        if (data.ping != -1L) {
            sb.append(data.ping).append("ms");
        }
        if (data.playerList != null) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(data.playerList.size());
        }
        if (data.motd != null && data.motd.getString() != null && !data.motd.getString().isEmpty()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(data.motd.getString());
        } else if (data.ip != null && !data.ip.isEmpty()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(data.ip);
        }
        return sb.toString();
    }

    public static void pingServers(Runnable onComplete) {
        Minecraft mc = Minecraft.getInstance();
        ServerList serverList = new ServerList(mc);
        serverList.load();
        ServerStatusPinger pinger = new ServerStatusPinger();
        for (int i = 0; i < serverList.size(); i++) {
            ServerData data = serverList.get(i);
            try {
                pinger.pingServer(data, () -> {
                    serverList.save();
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception e) {
                LOGGER.warn("Failed to ping server: {} ({})", data.name, data.ip);
                serverList.save();
            }
        }
        if (serverList.size() == 0 && onComplete != null) {
            onComplete.run();
        }
    }
}