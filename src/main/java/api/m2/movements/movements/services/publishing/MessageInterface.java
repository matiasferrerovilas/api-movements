package api.m2.movements.movements.services.publishing;

import api.m2.movements.movements.enums.EventType;

public interface MessageInterface {
    void publish(Object result, String topic, EventType eventType);
}
