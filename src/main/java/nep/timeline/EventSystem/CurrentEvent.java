package nep.timeline.EventSystem;

/**
 * Do not use!
 */
@Deprecated
public class CurrentEvent
{
    private static EventCore event = null;

    private CurrentEvent() {
    }

    public static void setEvent(EventCore event)
    {
        CurrentEvent.event = event;
    }

    public static EventCore getEvent()
    {
        return event;
    }
}
