# EventSystem

EventSystem is a lightweight Java 17 event handler framework with annotation-based listeners, priority ordering, and both synchronous and asynchronous dispatch.

**Features**
- Class-based event dispatch (handlers receive event subclasses)
- Optional event channels via `EventList`
- Priority ordering with `EventPriority`
- Sync or async dispatch through `EventBus`
- Cancellation support with `ignoreCancelled`
- Configurable error handling and executor setup

**Quick Start**
```java
public final class UserCreatedEvent extends EventCore {
    public UserCreatedEvent() {
        super(EventList.NONE);
    }
}

public final class UserListener {
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        System.out.println("User created: " + event);
    }
}

EventBus bus = EventBus.builder().build();
bus.register(new UserListener());
bus.post(new UserCreatedEvent());
bus.close();
```

**Default Bus Convenience**
```java
EventManager.addListener(new UserListener());
EventManager.call(new UserCreatedEvent());
EventManager.shutdown();
```

**Event Channels (`EventList`)**
```java
@EventListener(event = EventList.SHUT_DOWN)
public void onShutdown() {
    System.out.println("Shutdown received.");
}
```

**Async Dispatch**
```java
bus.postAsync(new UserCreatedEvent()).join();
```

**Cancellation**
```java
UserCreatedEvent event = new UserCreatedEvent();
event.setCancelled(true);

@EventListener(ignoreCancelled = true)
public void skippedWhenCancelled(UserCreatedEvent event) {
    // Will not run if event is cancelled.
}
```

**EventBus Configuration**
```java
EventBus bus = EventBus.builder()
        .stopOnCancelled(true)
        .daemonThreads(false)
        .build();
```

**It works**
```bash
./mvnw test
```
