package api.m2.movements.services.subscriptions;

import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionQueryService {
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<SubscriptionRecord> getSubscriptionsBy(List<String> currencySymbol, LocalDate lastPayment) {
        var user = userService.getAuthenticatedUserRecord();
        return subscriptionRepository.findByCurrencyAndLastPayment(user.id(), currencySymbol, lastPayment)
                .stream()
                .map(subscriptionMapper::toRecord)
                .toList();
    }
}
