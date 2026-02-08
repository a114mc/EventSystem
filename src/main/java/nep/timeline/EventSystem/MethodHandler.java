package nep.timeline.EventSystem;

import java.lang.reflect.Method;

public class MethodHandler {
    private final Method method;
    private final Object listener;

    public MethodHandler(Method method, Object listener) {
        this.method = method;
        this.listener = listener;
    }

    public Method getMethod() {
        return method;
    }

    public Object getListener() {
        return listener;
    }

    @Override
    public boolean equals(Object o2) {
        if (o2 instanceof MethodHandler) {
            MethodHandler methodHandler = (MethodHandler) o2;
            return methodHandler.getMethod().equals(method) && methodHandler.listener.equals(listener);
        }
        return false;
    }
}
