package api.m2.movements.services.banks;

import api.m2.movements.mappers.BankMapper;
import api.m2.movements.records.banks.BankRecord;
import api.m2.movements.repositories.UserBankRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankQueryService {

    private final UserBankRepository userBankRepository;
    private final UserService userService;
    private final BankMapper bankMapper;

    @Transactional(readOnly = true)
    public List<BankRecord> getAllBanks() {
        var user = userService.getAuthenticatedUser();
        return userBankRepository.findByUserId(user.getId())
                .stream()
                .map(ub -> bankMapper.toRecord(ub.getBank()))
                .toList();
    }
}
