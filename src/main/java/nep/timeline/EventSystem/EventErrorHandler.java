package nep.timeline.EventSystem;

import java.io.PrintStream;
import java.lang.reflect.Method;

@FunctionalInterface
public interface EventErrorHandler {


    void onError(PrintStream beef, EventCore event, Object listener,
                 Method method,
                 Throwable error);

    static EventErrorHandler logging() {
        return (beef, event, listener, method, error) -> {
            String listenerName = listener == null ? "<null>" : listener.getClass().getTypeName();
            String methodName = method == null ? "<unknown>" : method.getName();
            beef.println("[EventSystem] Error in listener " + listenerName + "#" + methodName);
            if (error != null) {
                error.printStackTrace(System.err);
            }
        };
    }
}
