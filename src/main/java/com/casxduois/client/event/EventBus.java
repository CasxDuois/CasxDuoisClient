package com.casxduois.client.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * EventBus siêu nhẹ, tối ưu bộ nhớ và CPU, không phụ thuộc thư viện nặng
 */
public class EventBus {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Subscribe {
    }

    private static class EventListener {
        private final Object target;
        private final Method method;

        public EventListener(Object target, Method method) {
            this.target = target;
            this.method = method;
            this.method.setAccessible(true);
        }

        public void invoke(Event event) {
            try {
                method.invoke(target, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final Map<Class<? extends Event>, List<EventListener>> listeners = new ConcurrentHashMap<>();

    public void register(Object subscriber) {
        for (Method method : subscriber.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(paramType)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
                    listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                             .add(new EventListener(subscriber, method));
                }
            }
        }
    }

    public void unregister(Object subscriber) {
        for (List<EventListener> list : listeners.values()) {
            list.removeIf(listener -> listener.target == subscriber);
        }
    }

    public void post(Event event) {
        List<EventListener> list = listeners.get(event.getClass());
        if (list != null && !list.isEmpty()) {
            for (EventListener listener : list) {
                listener.invoke(event);
                if (event.isCancelled()) {
                    break;
                }
            }
        }
    }
}
