package net.alan.gui.data.source;

import net.alan.gui.data.DynamicListData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.GameRules;

import java.util.*;

public class GameRulesDataSource {
    public static List<DynamicListData> load() {
        GameRules rules = GameRulesBridge.getCurrentRules();
        Minecraft mc = Minecraft.getInstance();
        List<DynamicListData> list = new ArrayList<>();

        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                String ruleId = key.getId();
                String translationKey = "gamerule." + ruleId;
                String displayName = I18n.get(translationKey);
                boolean currentValue = rules.getBoolean(key);
                String valueDisplay = currentValue ? I18n.get("gamerule.boolean.on") : I18n.get("gamerule.boolean.off");

                DynamicListData data = new DynamicListData.Builder(ruleId, displayName)
                        .description(valueDisplay)
                        .actionType("cycle_rule_value")
                        .joinable(true)
                        .build();
                list.add(data);
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                String ruleId = key.getId();
                String translationKey = "gamerule." + ruleId;
                String displayName = I18n.get(translationKey);
                int currentValue = rules.getInt(key);

                DynamicListData data = new DynamicListData.Builder(ruleId, displayName)
                        .description(String.valueOf(currentValue))
                        .actionType("cycle_rule_value")
                        .joinable(true)
                        .build();
                list.add(data);
            }
        });

        return list;
    }
}