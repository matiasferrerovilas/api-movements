package api.expenses.expenses.aspect;

import api.expenses.expenses.enums.EventType;
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
