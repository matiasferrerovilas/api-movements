package api.m2.movements.records.events;

import api.m2.movements.enums.EventType;

public record EventWrapper<T>(EventType eventType, T message) {
}
