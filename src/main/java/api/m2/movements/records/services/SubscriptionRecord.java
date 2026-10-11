package api.m2.movements.records.services;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import api.m2.movements.records.currencies.CurrencyRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;

@RegisterReflectionForBinding
public record SubscriptionRecord(Long id, String description, BigDecimal amount,
                            CurrencyRecord currency,
                            LocalDate lastPayment,
                            Boolean isPaid,
                            String workspaceName,
                            Long workspaceId,
                            String user) {

    @JsonIgnore
    @Override
    public Long workspaceId() {
        return workspaceId;
    }
}

