package nep.timeline.EventSystem.test;

import nep.timeline.EventSystem.*;
import nep.timeline.EventSystem.type.EventPriority;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusTest {
    static class SimpleEvent extends EventCore {
        SimpleEvent() {
            super(EventList.NONE);
        }
    }

    static class ShutdownEvent extends EventCore {
        ShutdownEvent() {
            super(EventList.SHUT_DOWN);
        }
    }

    @Test
    void dispatchByEventClass() {
        EventBus bus = EventBus.builder().build();
        List<String> calls = new ArrayList<>();

        Object listener = new Object() {
            @EventListener
            public void onSimple(SimpleEvent event) {
                calls.add("simple");
            }
        };

        bus.register(listener);
        bus.post(new SimpleEvent());

        assertEquals(List.of("simple"), calls);
        bus.close();
    }

    @Test
    void dispatchByEventList() {
        EventBus bus = EventBus.builder().build();
        AtomicInteger counter = new AtomicInteger(0);

        Object listener = new Object() {
            @EventListener(event = EventList.SHUT_DOWN)
            public void onShutdown() {
                counter.incrementAndGet();
            }
        };

        bus.register(listener);
        bus.post(new ShutdownEvent());

        assertEquals(1, counter.get());
        bus.close();
    }

    @Test
    void priorityOrderIsRespected() {
        EventBus bus = EventBus.builder().build();
        List<String> order = new ArrayList<>();

        Object listener = new Object() {
            @EventListener(priority = EventPriority.LOW)
            public void low(SimpleEvent event) {
                order.add("low");
            }

            @EventListener(priority = EventPriority.HIGH)
            public void high(SimpleEvent event) {
                order.add("high");
            }
        };

        bus.register(listener);
        bus.post(new SimpleEvent());

        assertEquals(List.of("high", "low"), order);
        bus.close();
    }

    @Test
    void ignoreCancelledSkipsListener() {
        EventBus bus = EventBus.builder().build();
        List<String> calls = new ArrayList<>();

        Object listener = new Object() {
            @EventListener(ignoreCancelled = true)
            public void ignored(SimpleEvent event) {
                calls.add("ignored");
            }

            @EventListener(ignoreCancelled = false)
            public void received(SimpleEvent event) {
                calls.add("received");
            }
        };

        bus.register(listener);
        SimpleEvent event = new SimpleEvent();
        event.setCancelled(true);
        bus.post(event);

        assertEquals(List.of("received"), calls);
        bus.close();
    }

    @Test
    void postAsyncRunsHandlers() throws Exception {
        EventBus bus = EventBus.builder().build();
        CountDownLatch latch = new CountDownLatch(1);

        Object listener = new Object() {
            @EventListener
            public void onSimple(SimpleEvent event) {
                latch.countDown();
            }
        };

        bus.register(listener);
        bus.postAsync(new SimpleEvent()).join();

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        bus.close();
    }

    @Test
    void invalidListenerSignatureThrows() {
        EventBus bus = EventBus.builder().build();

        Object listener = new Object() {
            @EventListener
            public void bad(SimpleEvent event, String extra) {
            }
        };

        assertThrows(EventException.class, () -> bus.register(listener));
        bus.close();
    }
}
