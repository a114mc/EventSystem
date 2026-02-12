package nep.timeline.EventSystem.example;

import nep.timeline.EventSystem.EventList;
import nep.timeline.EventSystem.EventListener;
import nep.timeline.EventSystem.EventManager;

public class SimpleExample {
    public static void main(String[] args) {
        EventManager.addListener(new SimpleExample());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.call(new ExampleShutdownEvent())));
        System.out.println("Hello world!");
        System.exit(0);
    }

    @EventListener(event = EventList.SHUT_DOWN)
    public void shutdown()
    {
        System.out.println("Shutdown.");
    }
    
    @EventListener
    public void shutdown(ExampleShutdownEvent event)
    {
        System.out.println("Shutdown..");
    }
}
