

package net.alan.gui.registry;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
public class RegisterScreenEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterScreenEvent.class);
    private static final List<Consumer<RegisterScreenEvent>> LISTENERS = new ArrayList<>();
    private static boolean fired = false;
    private final Map<String, Function<Screen, Screen>> entries = new HashMap<>();
    public void register(String screenId, Function<Screen, Screen> factory) {
        if (screenId == null || factory == null) {
            LOGGER.warn("Attempted to register screen with null screenId or factory");
            return;
        }
        if (entries.containsKey(screenId)) {
            LOGGER.warn("Overriding screen registration for screenId: {}", screenId);
        }
        entries.put(screenId, factory);
    }
    public void register(String screenId, Class<? extends Screen> screenClass) {
        if (screenId == null || screenClass == null) {
            LOGGER.warn("Attempted to register screen with null screenId or screenClass");
            return;
        }
        try {
            Constructor<? extends Screen> ctor = screenClass.getConstructor(Screen.class);
            register(screenId, parent -> {
                try {
                    return ctor.newInstance(parent);
                } catch (Exception e) {
                    LOGGER.error("Failed to instantiate screen class {} for screenId: {}",
                            screenClass.getName(), screenId, e);
                    return null;
                }
            });
        } catch (NoSuchMethodException e) {
            try {
                Constructor<? extends Screen> noArgCtor = screenClass.getConstructor();
                register(screenId, parent -> {
                    try {
                        return noArgCtor.newInstance();
                    } catch (Exception ex) {
                        LOGGER.error("Failed to instantiate screen class {} for screenId: {}",
                                screenClass.getName(), screenId, ex);
                        return null;
                    }
                });
            } catch (NoSuchMethodException ex) {
                LOGGER.error("Screen class {} has neither a (Screen) nor a () constructor",
                        screenClass.getName());
            }
        }
    }
    public static void addListener(Consumer<RegisterScreenEvent> listener) {
        if (fired) {
            LOGGER.warn("RegisterScreenEvent has already been fired; late listener will not be applied");
        }
        LISTENERS.add(listener);
    }
    static Map<String, Function<Screen, Screen>> fire() {
        fired = true;
        RegisterScreenEvent event = new RegisterScreenEvent();
        for (Consumer<RegisterScreenEvent> listener : LISTENERS) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in RegisterScreenEvent listener", e);
            }
        }
        return event.entries;
    }
}
