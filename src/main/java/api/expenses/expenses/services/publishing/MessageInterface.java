package api.expenses.expenses.services.publishing;

import api.expenses.expenses.enums.EventType;

public interface MessageInterface {
    void publish(Object result, String topic, EventType eventType);
}
