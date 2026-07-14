package api.m2.movements.configuration;

import api.m2.movements.constraints.CuotasValidator;
import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.investment.records.InvestmentRecord;
import api.m2.movements.investment.records.InvestmentTypeRecord;
import api.m2.movements.investment.services.valuation.YahooFinanceHttpClient;
import api.m2.movements.records.accounts.AccountBaseRecord;
import api.m2.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.events.EventWrapper;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.records.movements.file.MovementFileRequest;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.records.workspaces.WorkspacesWithUser;
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
            MovementFileRequest.class,
            SubscriptionRecord.class,
            CategoryRecord.class,
            CurrencyRecord.class,
            AccountBaseRecord.class,
            UserBaseRecord.class,
            WorkspaceDetail.class,
            WorkspacesWithUser.class,
            InvestmentRecord.class,
            InvestmentTypeRecord.class,
            YahooFinanceHttpClient.YahooChartResponse.class,
            YahooFinanceHttpClient.YahooChartResponse.Chart.class,
            YahooFinanceHttpClient.YahooChartResponse.ChartResult.class,
            YahooFinanceHttpClient.YahooChartResponse.Meta.class,
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
