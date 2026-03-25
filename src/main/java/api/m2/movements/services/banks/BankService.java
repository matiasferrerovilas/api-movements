package api.m2.movements.services.banks;

import api.m2.movements.mappers.BankMapper;
import api.m2.movements.records.banks.BankRecord;
import api.m2.movements.repositories.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository bankRepository;
    private final BankMapper bankMapper;

    public List<BankRecord> getAllBanks() {
        return bankMapper.toRecordList(bankRepository.findAll());
    }
}

