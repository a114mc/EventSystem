package nep.timeline.EventSystem;

import nep.timeline.EventSystem.type.EventPriority;

import java.lang.reflect.Method;
import java.util.Objects;

public final class MethodHandler {
    private final Method method;
    private final Object listener;
    private final EventList event;
    private final EventPriority priority;
    private final boolean async;
    private final boolean ignoreCancelled;
    private final Class<? extends EventCore> parameterType;

    private MethodHandler(Method method,
                          Object listener,
                          EventList event,
                          EventPriority priority,
                          boolean async,
                          boolean ignoreCancelled,
                          Class<? extends EventCore> parameterType) {
        this.method = method;
        this.listener = listener;
        this.event = event;
        this.priority = priority;
        this.async = async;
        this.ignoreCancelled = ignoreCancelled;
        this.parameterType = parameterType;
    }

    public static MethodHandler create(Object listener, Method method, EventListener annotation) {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(annotation, "annotation");

        int parameterCount = method.getParameterCount();
        if (parameterCount > 1) {
            throw new EventException("Listener methods must have 0 or 1 parameter. method name:" + method.getName());
        }

        Class<? extends EventCore> parameterType = null;
        if (parameterCount == 1) {
            Class<?> raw = method.getParameterTypes()[0];
            if (!EventCore.class.isAssignableFrom(raw)) {
                throw new EventException("Listener parameter must extend EventCore. method name:" + method.getName());
            }
            parameterType = raw.asSubclass(EventCore.class);
        } else if (annotation.event() == EventList.NONE) {
            throw new EventException("Listener methods without parameters must declare an event. method name:" + method.getName());
        }

        return new MethodHandler(
                method,
                listener,
                annotation.event(),
                annotation.priority(),
                annotation.async(),
                annotation.ignoreCancelled(),
                parameterType
        );
    }

    public Method getMethod() {
        return method;
    }

    public Object getListener() {
        return listener;
    }

    public EventList getEvent() {
        return event;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }

    public Class<? extends EventCore> getParameterType() {
        return parameterType;
    }

    public boolean matches(EventCore eventInstance) {
        if (ignoreCancelled && eventInstance.isCancelled()) {
            return false;
        }

        if (event != EventList.NONE && event != EventList.ALL && event != eventInstance.getEvent()) {
            return false;
        }

        if (parameterType == null) {
            return true;
        }

        return parameterType.isAssignableFrom(eventInstance.getClass());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodHandler)) {
            return false;
        }
        MethodHandler that = (MethodHandler) other;
        return method.equals(that.method) && listener == that.listener;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(listener) + method.hashCode();
    }
}
