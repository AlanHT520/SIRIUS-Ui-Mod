package net.alan.gui.render.card;

import net.alan.gui.data.Action;
import net.alan.gui.render.ActionExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class CardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CardManager.class);

    private final Deque<CardInstance> stack = new LinkedList<>();
    private final Map<String, CardDefinition> registeredCards = new HashMap<>();
    private final Minecraft minecraft;
    private final ActionExecutor executor;

    public CardManager(Minecraft minecraft, ActionExecutor executor) {
        this.minecraft = minecraft;
        this.executor = executor;
    }

    public void showCard(String cardId, String instanceId, Map<String, String> params) {
        CardDefinition def = registeredCards.get(cardId);
        if (def == null) {
            def = CardRegistry.get(minecraft.getResourceManager(), cardId);
        }
        if (def == null) {
            LOGGER.error("Cannot show card: definition not found for {}", cardId);
            return;
        }
        showCardWithDef(def, instanceId, params, null);
    }

    public void showCardWithConfirm(String cardId, String instanceId,
                                    Map<String, String> params, Action pendingAction) {
        CardDefinition def = registeredCards.get(cardId);
        if (def == null) {
            def = CardRegistry.get(minecraft.getResourceManager(), cardId);
        }
        if (def == null) {
            LOGGER.error("Cannot show card: definition not found for {}", cardId);
            return;
        }
        showCardWithDef(def, instanceId, params, pendingAction);
    }

    public void registerCardDefinition(String id, CardDefinition def) {
        registeredCards.put(id, def);
        LOGGER.debug("Registered card definition: {}", id);
    }

    public void clearRegisteredCards() {
        registeredCards.clear();
        LOGGER.debug("Cleared registered card definitions");
    }

    public void showCardFromJson(com.google.gson.JsonObject cardObj, String instanceId,
                                 Map<String, String> params) {
        CardDefinition def = CardRegistry.createFromJson(cardObj);
        if (def == null) {
            LOGGER.error("Cannot show card: failed to parse inline definition");
            return;
        }
        showCardWithDef(def, instanceId, params, null);
    }

    private void showCardWithDef(CardDefinition def, String instanceId,
                                 Map<String, String> params, Action pendingAction) {
        CardInstance instance = new CardInstance(instanceId, def, params,
                minecraft.getResourceManager(), executor);
        instance.setPendingAction(pendingAction);
        dismiss(instanceId);
        stack.push(instance);
    }

    public void showToast(String id, String message, int durationMs) {
        CardDefinition def = registeredCards.get(id);
        if (def != null) {
            Map<String, String> params = new HashMap<>();
            params.put("message", message);
            showCardWithDef(def, id, params, null);
            return;
        }
        def = new CardDefinition();
        def.setType("toast");
        def.setModal(false);
        def.setDurationMs(durationMs);
        def.setWidth(Math.min(300, minecraft.getWindow().getGuiScaledWidth() - 20));
        def.setMinHeight(24);
        def.setPadding(8);
        def.setBorder("0xFF555555");
        Map<String, String> params = new HashMap<>();
        params.put("message", message);
        showCardWithDef(def, id, params, null);
    }

    public void showLoading(String id, String message) {
        CardDefinition def = registeredCards.get(id);
        if (def != null) {
            Map<String, String> params = new HashMap<>();
            params.put("message", message != null ? message : "Loading...");
            showCardWithDef(def, id, params, null);
            return;
        }
        def = new CardDefinition();
        def.setType("loading");
        def.setModal(true);
        def.setWidth(200);
        def.setMinHeight(40);
        def.setPadding(16);
        def.setOverlay("0xCC1A1A1A");
        def.setBorder("0xFF555555");
        Map<String, String> params = new HashMap<>();
        params.put("message", message != null ? message : "Loading...");
        showCardWithDef(def, id, params, null);
    }

    public void showTooltip(String id, String message, int x, int y) {
        CardDefinition def = registeredCards.get(id);
        if (def != null) {
            Map<String, String> params = new HashMap<>();
            params.put("message", message);
            showCardWithDef(def, id, params, null);
            return;
        }
        def = new CardDefinition();
        def.setType("tooltip");
        def.setModal(false);
        def.setWidth(minecraft.font.width(message) + 12);
        def.setMinHeight(minecraft.font.lineHeight + 8);
        def.setPadding(6);
        def.setBorder("0xFF555555");
        def.setPosX(x);
        def.setPosY(y);
        Map<String, String> params = new HashMap<>();
        params.put("message", message);
        showCardWithDef(def, id, params, null);
    }

    public void dismiss(String id) {
        Iterator<CardInstance> it = stack.iterator();
        while (it.hasNext()) {
            CardInstance instance = it.next();
            if (instance != null && id.equals(instance.getId())) {
                it.remove();
                if (instance.getOnDismiss() != null) {
                    instance.getOnDismiss().run();
                }
                return;
            }
        }
    }

    public void dismissAll() {
        while (!stack.isEmpty()) {
            CardInstance instance = stack.pop();
            if (instance != null && instance.getOnDismiss() != null) {
                instance.getOnDismiss().run();
            }
        }
    }

    public boolean isModalActive() {
        for (CardInstance instance : stack) {
            if (instance != null && instance.isModal()) return true;
        }
        return false;
    }

    public boolean hasActive() {
        return !stack.isEmpty();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (stack.isEmpty()) return;

        int zIndex = 400;
        CardInstance first = stack.peekFirst();
        if (first != null) {
            zIndex = first.getDefinition().getZIndex();
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zIndex);

        Iterator<CardInstance> it = stack.iterator();
        while (it.hasNext()) {
            CardInstance instance = it.next();

            if (instance.isExpired()) {
                it.remove();
                if (instance.getOnDismiss() != null) {
                    instance.getOnDismiss().run();
                }
                continue;
            }

            instance.render(graphics, mouseX, mouseY, delta);
        }

        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (stack.isEmpty()) return false;

        CardInstance top = stack.peek();
        if (top != null) {
            return top.mouseClicked(mx, my, button);
        }

        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (stack.isEmpty()) return false;

        CardInstance top = stack.peek();
        if (top != null) {
            return top.mouseReleased(mx, my, button);
        }

        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (stack.isEmpty()) return false;

        CardInstance top = stack.peek();
        if (top != null) {
            return top.mouseDragged(mx, my, button, dragX, dragY);
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (stack.isEmpty()) return false;

        CardInstance top = stack.peek();
        if (top != null) {
            if (top.isModal() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
                dismissAll();
                return true;
            }
            return top.keyPressed(keyCode, scanCode, modifiers);
        }

        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (stack.isEmpty()) return false;

        CardInstance top = stack.peek();
        if (top != null) {
            return top.charTyped(codePoint, modifiers);
        }

        return false;
    }

    public String getInputValue(String overlayId, String fieldId) {
        for (CardInstance instance : stack) {
            if (instance != null && instance.getId().equals(overlayId)) {
                return instance.getInputValue(fieldId);
            }
        }
        return null;
    }

    public Action getTopPendingAction() {
        CardInstance top = stack.peek();
        if (top != null) {
            return top.getPendingAction();
        }
        return null;
    }

    public String getTopCardParam(String key) {
        for (CardInstance instance : stack) {
            if (instance != null) {
                String value = instance.getParams().get(key);
                if (value != null) return value;
            }
        }
        return null;
    }
}