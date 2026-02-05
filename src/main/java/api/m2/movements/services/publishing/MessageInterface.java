package api.m2.movements.services.publishing;

import api.m2.movements.enums.EventType;

public interface MessageInterface {
    void publish(Object result, String topic, EventType eventType);
}
