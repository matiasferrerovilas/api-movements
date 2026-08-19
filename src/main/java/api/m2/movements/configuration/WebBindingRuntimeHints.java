package api.m2.movements.configuration;

import api.m2.movements.constraints.CuotasValidator;
import api.m2.movements.clients.identity.response.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.events.EventWrapper;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Bajo native-image hace falta reflection registrada explícitamente para:
 * records bindeados como {@code @ParameterObject} (query params), records
 * serializados solo vía WebSocket/AMQP o deserializados solo desde clientes
 * HTTP salientes ({@code @HttpExchange}) — nunca pasan por un controller,
 * así que el escaneo AOT de Spring MVC no los detecta solo — y
 * {@code ConstraintValidator} custom.
 */
public class WebBindingRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] RECORD_TYPES = {
            BalanceFilterRecord.class,
            MovementSearchFilterRecord.class,
            EventWrapper.class,
            MovementRecord.class,
            MovementRecord.Metadata.class,
            SubscriptionRecord.class,
            CategoryRecord.class,
            CurrencyRecord.class,
            WorkspaceBaseRecord.class,
            UserBaseRecord.class,
            WorkspaceDetail.class,
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> type : RECORD_TYPES) {
            hints.reflection().registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }
        hints.reflection().registerType(CuotasValidator.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
