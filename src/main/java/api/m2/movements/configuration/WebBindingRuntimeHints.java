package api.m2.movements.configuration;

import api.m2.movements.constraints.CuotasValidator;
import api.m2.movements.identity.records.invite.InvitationToWorkspaceRecord;
import api.m2.movements.identity.records.users.UserBaseRecord;
import api.m2.movements.identity.records.workspaces.WorkspaceDetail;
import api.m2.movements.identity.records.workspaces.WorkspaceMemberRecord;
import api.m2.movements.identity.records.workspaces.WorkspaceRecord;
import api.m2.movements.investment.records.InvestmentRecord;
import api.m2.movements.investment.records.InvestmentTypeRecord;
import api.m2.movements.movements.records.accounts.AccountBaseRecord;
import api.m2.movements.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.movements.records.categories.CategoryRecord;
import api.m2.movements.movements.records.currencies.CurrencyRecord;
import api.m2.movements.movements.records.events.EventWrapper;
import api.m2.movements.movements.records.movements.MovementRecord;
import api.m2.movements.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.movements.records.movements.file.MovementFileRequest;
import api.m2.movements.movements.records.services.SubscriptionRecord;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Bajo native-image hace falta reflection registrada explícitamente para:
 * records bindeados como {@code @ParameterObject} (query params), records
 * serializados solo vía WebSocket/AMQP (nunca devueltos por un controller,
 * así que el escaneo AOT de Spring MVC no los detecta solo) y
 * {@code ConstraintValidator} custom.
 */
public class WebBindingRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] RECORD_TYPES = {
            BalanceFilterRecord.class,
            MovementSearchFilterRecord.class,
            EventWrapper.class,
            MovementRecord.class,
            MovementFileRequest.class,
            SubscriptionRecord.class,
            CategoryRecord.class,
            CurrencyRecord.class,
            AccountBaseRecord.class,
            UserBaseRecord.class,
            InvitationToWorkspaceRecord.class,
            WorkspaceRecord.class,
            WorkspaceDetail.class,
            WorkspaceMemberRecord.class,
            InvestmentRecord.class,
            InvestmentTypeRecord.class,
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> type : RECORD_TYPES) {
            hints.reflection().registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);
        }
        hints.reflection().registerType(CuotasValidator.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
