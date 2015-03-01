package com.oberasoftware.home.core.events;

import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
public class HandlerEntry {
    private static final Logger LOG = getLogger(HandlerEntry.class);

    private Method eventMethod;
    private Object listenerInstance;

    public HandlerEntry(Object listenerInstance, Method eventMethod) {
        this.eventMethod = eventMethod;
        this.listenerInstance = listenerInstance;
    }

    public Optional<?> executeHandler(Object event) {
        LOG.debug("Executing event listener: {} method: {} for event: {}", listenerInstance.getClass().getSimpleName(), eventMethod.getName(), event);
        try {
            return Optional.ofNullable(eventMethod.invoke(listenerInstance, event));
        } catch (Throwable e) {
            LOG.error("Unable to execute listener instance", e);
        }

        return Optional.empty();
    }

    public Method getEventMethod() {
        return eventMethod;
    }

    public Object getListenerInstance() {
        return listenerInstance;
    }
}
