package net.alan.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;

public class GameStateProvider {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GameStateProvider.class);

    public static int resolve(String key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return 0;

        return switch (key) {
            case "game.in_level" -> mc.level != null ? 1 : 0;
            case "game.is_singleplayer" -> mc.isSingleplayer() ? 1 : 0;
            case "game.is_multiplayer" -> (mc.level != null && !mc.isSingleplayer()) ? 1 : 0;
            case "game.is_integrated_server" -> mc.hasSingleplayerServer() ? 1 : 0;
            case "game.is_dedicated_server" -> (mc.getConnection() != null && !mc.isSingleplayer() && !mc.hasSingleplayerServer()) ? 1 : 0;
            case "game.is_hardcore" -> mc.level != null && mc.level.getLevelData().isHardcore() ? 1 : 0;
            case "game.is_flat" -> 0; // ClientLevelData 未暴露 isFlat，客户端无法获取
            case "game.is_debug" -> mc.level != null && mc.level.isDebug() ? 1 : 0;
            case "game.is_demo" -> mc.isDemo() ? 1 : 0;
            case "game.is_paused" -> mc.isPaused() ? 1 : 0;

            case "game.is_creative" -> getGameType(mc) == GameType.CREATIVE ? 1 : 0;
            case "game.is_survival" -> getGameType(mc) == GameType.SURVIVAL ? 1 : 0;
            case "game.is_spectator" -> getGameType(mc) == GameType.SPECTATOR ? 1 : 0;
            case "game.is_adventure" -> getGameType(mc) == GameType.ADVENTURE ? 1 : 0;
            case "game.game_mode" -> getGameType(mc).ordinal();

            case "game.difficulty" -> mc.level != null ? mc.level.getDifficulty().ordinal() : 0;
            case "game.is_hard_difficulty" -> mc.level != null && mc.level.getDifficulty().ordinal() == 3 ? 1 : 0;
            case "game.can_modify_difficulty" -> {
                if (mc.level == null) yield 0;
                if (mc.hasSingleplayerServer()) {
                    IntegratedServer server = mc.getSingleplayerServer();
                    yield server != null && server.getWorldData().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL ? 1 : 0;
                }
                yield mc.level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL ? 1 : 0;
            }
            case "game.is_difficulty_locked" -> mc.level != null && mc.level.getLevelData().isDifficultyLocked() ? 1 : 0;

            case "game.has_cheats" -> {
                if (mc.player == null) yield 0;
                if (mc.hasSingleplayerServer()) {
                    IntegratedServer server = mc.getSingleplayerServer();
                    yield server != null && server.getWorldData().isAllowCommands() ? 1 : 0;
                }
                yield mc.player.hasPermissions(2) ? 1 : 0;
            }
            case "game.is_host" -> {
                if (mc.hasSingleplayerServer()) {
                    IntegratedServer server = mc.getSingleplayerServer();
                    yield server != null && server.isPublished() ? 1 : 0;
                }
                yield 0;
            }
            case "game.is_op" -> mc.player != null && mc.player.hasPermissions(2) ? 1 : 0;

            case "game.is_connected" -> mc.getConnection() != null ? 1 : 0;
            case "game.is_realm" -> 0; // 1.21.1 没有公开 API 判断是否连接到 Realm，默认为 false
            case "game.player_count" -> mc.level != null ? mc.level.players().size() : 0;

            default -> {
                LOGGER.debug("Unknown game state variable: {}", key);
                yield 0;
            }
        };
    }

    private static GameType getGameType(Minecraft mc) {
        if (mc.player == null && mc.level == null) return GameType.SURVIVAL;
        if (mc.gameMode != null) {
            return mc.gameMode.getPlayerMode();
        }
        return GameType.SURVIVAL;
    }
}