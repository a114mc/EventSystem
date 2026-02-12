package nep.timeline.EventSystem;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"UnusedReturnValue","unused"})
public class EventManager
{
    private static final EventBus DEFAULT_BUS = EventBus.builder().build();

    private EventManager() {
    }

    public static void reset()
    {
        DEFAULT_BUS.clear();
    }

    public static void addListener(Object... projects)
    {
        DEFAULT_BUS.register(projects);
    }

    public static void removeListener(Object... projects)
    {
        DEFAULT_BUS.unregister(projects);
    }

    public static EventCore call(EventCore event)
    {
        return call(event, false);
    }

    public static EventCore call(EventCore event, boolean multiThread)
    {
        if (multiThread) {
            return DEFAULT_BUS.postAsync(event).join();
        }
        return DEFAULT_BUS.post(event);
    }

    public static CompletableFuture<EventCore> callAsync(EventCore event) {
        return DEFAULT_BUS.postAsync(event);
    }

    public static EventBus getDefaultBus() {
        return DEFAULT_BUS;
    }

    public static void shutdown() {
        DEFAULT_BUS.close();
    }
}
