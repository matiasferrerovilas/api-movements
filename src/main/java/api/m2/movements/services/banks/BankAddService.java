package api.m2.movements.services.banks;

import api.m2.movements.entities.commons.Bank;
import api.m2.movements.entities.integrity.UserBank;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.BankMapper;
import api.m2.movements.records.banks.BankRecord;
import api.m2.movements.records.banks.BankResolutionResult;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.UserBankRepository;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAddService {

    private final BankRepository bankRepository;
    private final UserBankRepository userBankRepository;
    private final UserService userService;
    private final BankMapper bankMapper;
    private final UserSettingService userSettingService;

    @Transactional
    public BankRecord addBankToUser(String description) {
        var userId = userService.getMe().id();
        var result = this.resolveUserBank(description, userId);

        // Si es el único banco del usuario Y fue recién agregado, establecerlo como default automáticamente
        if (result.wasAdded()) {
            List<UserBank> userBanks = userBankRepository.findByUserId(userId);
            if (userBanks.size() == 1) {
                log.debug("Setting bank {} as default for user {} (first bank)", result.bank().getId(), userId);
                userSettingService.upsertForUser(userId, UserSettingKey.DEFAULT_BANK, result.bank().getId());
            }
        }

        return bankMapper.toRecord(result.bank());
    }

    @Transactional
    public Bank addBankToUser(String description, Long userId) {
        return this.resolveUserBank(description, userId).bank();
    }


    @Transactional
    public Map<String, Bank> addBanksToUser(List<String> descriptions, Long userId) {
        var sanitizedByOriginal = descriptions.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        description -> description.trim().toUpperCase(),
                        (first, second) -> first,
                        LinkedHashMap::new));

        var banksByDescription = bankRepository.findByDescriptionIn(sanitizedByOriginal.values()).stream()
                .collect(Collectors.toMap(Bank::getDescription, Function.identity()));

        var newBanks = sanitizedByOriginal.values().stream()
                .distinct()
                .filter(description -> !banksByDescription.containsKey(description))
                .map(description -> Bank.builder().description(description).build())
                .toList();
        bankRepository.saveAll(newBanks)
                .forEach(bank -> banksByDescription.put(bank.getDescription(), bank));

        var bankIds = banksByDescription.values().stream().map(Bank::getId).toList();
        userBankRepository.linkBanksToUser(userId, bankIds);

        return sanitizedByOriginal.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> banksByDescription.get(entry.getValue())));
    }

    private BankResolutionResult resolveUserBank(String description, Long userId) {
        String sanitized = description.trim().toUpperCase();

        Bank bank = bankRepository.findByDescription(sanitized)
                .orElseGet(() -> bankRepository.save(Bank.builder().description(sanitized).build()));

        boolean wasAdded = false;
        if (!userBankRepository.existsByUserIdAndBankId(userId, bank.getId())) {
            userBankRepository.save(UserBank.builder().userId(userId).bank(bank).build());
            wasAdded = true;
        }

        return new BankResolutionResult(bank, wasAdded);
    }

    @Transactional
    public void removeBankFromUser(Long bankId) {
        var userId = userService.getMe().id();

        if (!userBankRepository.existsByUserIdAndBankId(userId, bankId)) {
            throw new EntityNotFoundException("El banco con id " + bankId + " no está en la lista del usuario");
        }

        userBankRepository.deleteByUserIdAndBankId(userId, bankId);

        // Gestionar DEFAULT_BANK según los bancos restantes
        List<UserBank> remainingBanks = userBankRepository.findByUserId(userId);

        if (remainingBanks.isEmpty()) {
            // No quedan bancos: eliminar el setting DEFAULT_BANK
            log.debug("User {} has no banks left, clearing DEFAULT_BANK setting", userId);
            userSettingService.deleteByKey(UserSettingKey.DEFAULT_BANK);
        } else if (remainingBanks.size() == 1) {
            // Queda exactamente 1 banco: establecerlo como default
            Long remainingBankId = remainingBanks.get(0).getBank().getId();
            log.debug("Setting bank {} as default for user {} (only remaining bank)", remainingBankId, userId);
            userSettingService.upsertForUser(userId, UserSettingKey.DEFAULT_BANK, remainingBankId);
        }
        // Si quedan 2 o más bancos: no hacer nada, mantener el default actual
    }
}
