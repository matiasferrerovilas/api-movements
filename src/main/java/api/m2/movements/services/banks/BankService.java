package api.m2.movements.services.banks;

import api.m2.movements.entities.Bank;
import api.m2.movements.entities.UserBank;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.BankMapper;
import api.m2.movements.records.banks.BankRecord;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.UserBankRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository bankRepository;
    private final UserBankRepository userBankRepository;
    private final UserService userService;
    private final BankMapper bankMapper;

    public List<BankRecord> getAllBanks() {
        var user = userService.getAuthenticatedUser();
        return userBankRepository.findByUserId(user.getId())
                .stream()
                .map(ub -> bankMapper.toRecord(ub.getBank()))
                .toList();
    }

    @Transactional
    public BankRecord addBankToUser(String description) {
        String sanitized = description.trim().toUpperCase();

        var user = userService.getAuthenticatedUser();

        Bank bank = bankRepository.findByDescription(sanitized)
                .orElseGet(() -> bankRepository.save(Bank.builder().description(sanitized).build()));

        if (!userBankRepository.existsByUserIdAndBankId(user.getId(), bank.getId())) {
            userBankRepository.save(UserBank.builder().user(user).bank(bank).build());
        }

        return bankMapper.toRecord(bank);
    }

    @Transactional
    public void removeBankFromUser(Long bankId) {
        var user = userService.getAuthenticatedUser();

        if (!userBankRepository.existsByUserIdAndBankId(user.getId(), bankId)) {
            throw new EntityNotFoundException("El banco con id " + bankId + " no está en la lista del usuario");
        }

        userBankRepository.deleteByUserIdAndBankId(user.getId(), bankId);
    }
}
