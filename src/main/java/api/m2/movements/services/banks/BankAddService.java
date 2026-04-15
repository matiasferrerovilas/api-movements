package api.m2.movements.services.banks;

import api.m2.movements.entities.Bank;
import api.m2.movements.entities.User;
import api.m2.movements.entities.UserBank;
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

import java.util.List;

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
        var user = userService.getAuthenticatedUser();
        var result = this.resolveUserBank(description, user);

        // Si es el único banco del usuario Y fue recién agregado, establecerlo como default automáticamente
        if (result.wasAdded()) {
            List<UserBank> userBanks = userBankRepository.findByUserId(user.getId());
            if (userBanks.size() == 1) {
                log.debug("Setting bank {} as default for user {} (first bank)", result.bank().getId(), user.getId());
                userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, result.bank().getId());
            }
        }

        return bankMapper.toRecord(result.bank());
    }

    @Transactional
    public Bank addBankToUser(String description, User user) {
        return this.resolveUserBank(description, user).bank();
    }

    private BankResolutionResult resolveUserBank(String description, User user) {
        String sanitized = description.trim().toUpperCase();

        Bank bank = bankRepository.findByDescription(sanitized)
                .orElseGet(() -> bankRepository.save(Bank.builder().description(sanitized).build()));

        boolean wasAdded = false;
        if (!userBankRepository.existsByUserIdAndBankId(user.getId(), bank.getId())) {
            userBankRepository.save(UserBank.builder().user(user).bank(bank).build());
            wasAdded = true;
        }

        return new BankResolutionResult(bank, wasAdded);
    }

    @Transactional
    public void removeBankFromUser(Long bankId) {
        var user = userService.getAuthenticatedUser();

        if (!userBankRepository.existsByUserIdAndBankId(user.getId(), bankId)) {
            throw new EntityNotFoundException("El banco con id " + bankId + " no está en la lista del usuario");
        }

        userBankRepository.deleteByUserIdAndBankId(user.getId(), bankId);

        // Gestionar DEFAULT_BANK según los bancos restantes
        List<UserBank> remainingBanks = userBankRepository.findByUserId(user.getId());

        if (remainingBanks.isEmpty()) {
            // No quedan bancos: eliminar el setting DEFAULT_BANK
            log.debug("User {} has no banks left, clearing DEFAULT_BANK setting", user.getId());
            userSettingService.deleteByKey(UserSettingKey.DEFAULT_BANK);
        } else if (remainingBanks.size() == 1) {
            // Queda exactamente 1 banco: establecerlo como default
            Long remainingBankId = remainingBanks.get(0).getBank().getId();
            log.debug("Setting bank {} as default for user {} (only remaining bank)", remainingBankId, user.getId());
            userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, remainingBankId);
        }
        // Si quedan 2 o más bancos: no hacer nada, mantener el default actual
    }
}
