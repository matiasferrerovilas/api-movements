package api.m2.movements.movements.records.events;

import api.m2.movements.movements.enums.EventType;

public record EventWrapper<T>(EventType eventType, T message) {
}
