package ch.fhnw.crowpi.components.events;

/**
 * Generic functional interface for simple event handlers.
 * Usually supposed to be called / triggered within {@link SimpleEventProvider#dispatchSimpleEvents(EventListener, Object)}
 */
@FunctionalInterface
public interface SimpleEventHandler {
    /**
     * Handles a specific simple event based on implementation needs.
     * This method does not take any parameters and returns no value either.
     * For more advanced event handling, use {@link EventHandler}.
     */
    void handle();
}
