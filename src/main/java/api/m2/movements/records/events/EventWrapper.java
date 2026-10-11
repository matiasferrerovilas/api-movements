package api.m2.movements.records.events;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import api.m2.movements.enums.EventType;

@RegisterReflectionForBinding
public record EventWrapper<T>(EventType eventType, T message) {
}
