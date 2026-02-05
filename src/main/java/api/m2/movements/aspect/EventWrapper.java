package api.m2.movements.aspect;

import api.m2.movements.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventWrapper<T> {
    private EventType eventType;
    private T message;
}
