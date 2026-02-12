package nep.timeline.EventSystem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class EventBus implements AutoCloseable {
    private static final Comparator<MethodHandler> PRIORITY_ORDER = Comparator
            .comparingInt((MethodHandler handler) -> handler.getPriority().getLevel())
            .thenComparing(handler -> handler.getListener().getClass().getName())
            .thenComparing(handler -> handler.getMethod().getName());

    private final Object lock = new Object();
    private final ExecutorService executor;
    private final boolean ownsExecutor;
    private final boolean stopOnCancelled;
    private final EventErrorHandler errorHandler;
    private volatile List<MethodHandler> handlers = List.of();

    public EventBus() {
        this(Executors.newCachedThreadPool(namedThreadFactory("event-bus-", true)), true, false, EventErrorHandler.logging());
    }

    public EventBus(ExecutorService executorService) {
        this(executorService, false, false, EventErrorHandler.logging());
    }

    private EventBus(ExecutorService executorService,
                     boolean ownsExecutor,
                     boolean stopOnCancelled,
                     EventErrorHandler errorHandler) {
        this.executor = Objects.requireNonNull(executorService, "executorService");
        this.ownsExecutor = ownsExecutor;
        this.stopOnCancelled = stopOnCancelled;
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public static Builder builder() {
        return new Builder();
    }

    public void register(Object... listeners) {
        if (listeners == null || listeners.length == 0) {
            return;
        }

        List<MethodHandler> additions = new ArrayList<>();
        for (Object listener : listeners) {
            if (listener == null) {
                continue;
            }
            additions.addAll(collectHandlers(listener));
        }

        if (additions.isEmpty()) {
            return;
        }

        synchronized (lock) {
            List<MethodHandler> updated = new ArrayList<>(handlers);
            for (MethodHandler handler : additions) {
                if (!updated.contains(handler)) {
                    updated.add(handler);
                }
            }
            updated.sort(PRIORITY_ORDER);
            handlers = Collections.unmodifiableList(updated);
        }
    }

    public void unregister(Object... listeners) {
        if (listeners == null || listeners.length == 0) {
            return;
        }

        Set<Object> toRemove = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object listener : listeners) {
            if (listener != null) {
                toRemove.add(listener);
            }
        }

        synchronized (lock) {
            if (handlers.isEmpty()) {
                return;
            }
            List<MethodHandler> updated = handlers.stream()
                    .filter(handler -> !toRemove.contains(handler.getListener()))
                    .collect(Collectors.toCollection(ArrayList::new));
            handlers = Collections.unmodifiableList(updated);
        }
    }

    public void clear() {
        synchronized (lock) {
            handlers = List.of();
        }
    }

    public EventCore post(EventCore event) {
        dispatch(event, false).join();
        return event;
    }

    public CompletableFuture<EventCore> postAsync(EventCore event) {
        return dispatch(event, true).thenApply(ignored -> event);
    }

    private CompletableFuture<Void> dispatch(EventCore event, boolean asyncDispatch) {
        Objects.requireNonNull(event, "event");

        List<MethodHandler> snapshot = handlers;
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (MethodHandler handler : snapshot) {
            if (stopOnCancelled && event.isCancelled()) {
                break;
            }
            if (!handler.matches(event)) {
                continue;
            }

            Runnable task = () -> invoke(handler, event);
            if (asyncDispatch || handler.isAsync()) {
                futures.add(CompletableFuture.runAsync(task, executor));
            } else {
                task.run();
            }
        }

        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void invoke(MethodHandler handler, EventCore event) {
        try {
            Method method = handler.getMethod();
            Object listener = handler.getListener();
            if (!method.canAccess(listener)) {
                method.setAccessible(true);
            }
            if (handler.getParameterType() == null) {
                method.invoke(listener);
            } else {
                method.invoke(listener, event);
            }
        } catch (Throwable throwable) {
            Throwable cause = throwable;
            if (throwable instanceof java.lang.reflect.InvocationTargetException && throwable.getCause() != null) {
                cause = throwable.getCause();
            }
            errorHandler.onError(System.err, event, handler.getListener(),
                    handler.getMethod(), cause);
        }
    }

    @Override
    public void close() {
        if (ownsExecutor) {
            executor.shutdown();
        }
    }

    private static List<MethodHandler> collectHandlers(Object listener) {
        List<MethodHandler> collected = new ArrayList<>();
        Class<?> type = listener.getClass();
        while (type != null && type != Object.class) {
            for (Method method : type.getDeclaredMethods()) {
                EventListener annotation = method.getDeclaredAnnotation(EventListener.class);
                if (annotation == null) {
                    continue;
                }
                collected.add(MethodHandler.create(listener, method, annotation));
            }
            type = type.getSuperclass();
        }
        return collected;
    }

    private static ThreadFactory namedThreadFactory(String prefix, boolean daemon) {
        AtomicInteger index = new AtomicInteger(0);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + index.incrementAndGet());
            thread.setDaemon(daemon);
            return thread;
        };
    }

    public static final class Builder {
        private ExecutorService executorService;
        private boolean stopOnCancelled;
        private EventErrorHandler errorHandler = EventErrorHandler.logging();
        private String threadNamePrefix = "event-bus-";
        private boolean daemonThreads = true;

        public Builder executor(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder stopOnCancelled(boolean stopOnCancelled) {
            this.stopOnCancelled = stopOnCancelled;
            return this;
        }

        public Builder errorHandler(EventErrorHandler errorHandler) {
            this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
            return this;
        }

        public Builder threadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");
            return this;
        }

        public Builder daemonThreads(boolean daemonThreads) {
            this.daemonThreads = daemonThreads;
            return this;
        }

        public EventBus build() {
            if (executorService != null) {
                return new EventBus(executorService, false, stopOnCancelled, errorHandler);
            }
            ExecutorService executor = Executors.newCachedThreadPool(namedThreadFactory(threadNamePrefix, daemonThreads));
            return new EventBus(executor, true, stopOnCancelled, errorHandler);
        }
    }
}
