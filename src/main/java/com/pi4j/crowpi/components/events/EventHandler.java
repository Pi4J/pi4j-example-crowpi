package com.pi4j.crowpi.components.events;

/**
 * Generic functional interface for event handlers called by component event listeners.
 *
 * @param <V> Type of value which gets passed once an event occurs.
 */
@FunctionalInterface
public interface EventHandler<V> {
    /**
     * Handles a specific event based on implementation needs.
     *
     * @param listener Event listener which triggered this handler
     * @param value    Event value
     */
    void handle(EventListener listener, V value);
}
