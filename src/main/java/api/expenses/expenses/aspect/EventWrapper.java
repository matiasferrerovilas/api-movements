package api.expenses.expenses.aspect;

import api.expenses.expenses.enums.EventType;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventWrapper<T> {
    private EventType eventType;
    private T message;
}
