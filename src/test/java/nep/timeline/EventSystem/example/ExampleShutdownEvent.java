package nep.timeline.EventSystem.example;

import nep.timeline.EventSystem.EventCore;
import nep.timeline.EventSystem.EventList;

public class ExampleShutdownEvent extends EventCore
{
    public ExampleShutdownEvent()
    {
        super(EventList.SHUT_DOWN);
    }
}
