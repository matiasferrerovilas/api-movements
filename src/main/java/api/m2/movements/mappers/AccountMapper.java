package api.m2.movements.mappers;

import api.m2.movements.entities.Account;
import api.m2.movements.records.accounts.AccountRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface AccountMapper {

    AccountRecord toRecord(Account account);
    List<AccountRecord> toRecord(List<Account> account);
}
