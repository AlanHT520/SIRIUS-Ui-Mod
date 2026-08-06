package net.alan.gui.data.source;

import net.minecraft.world.level.GameRules;

import java.util.HashMap;
import java.util.Map;

public class GameRulesBridge {
    private static GameRules currentRules = new GameRules();
    private static final Map<String, GameRules.Key<?>> keyMap = new HashMap<>();

    static {
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                keyMap.put(key.getId(), key);
            }
        });
    }

    public static GameRules getCurrentRules() {
        return currentRules;
    }

    public static void setCurrentRules(GameRules rules) {
        currentRules = rules.copy();
    }

    public static void reset() {
        currentRules = new GameRules();
    }

    @SuppressWarnings("unchecked")
    public static <T extends GameRules.Value<T>> GameRules.Key<T> getKey(String ruleId) {
        return (GameRules.Key<T>) keyMap.get(ruleId);
    }
}