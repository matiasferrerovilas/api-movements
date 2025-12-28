package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Account;
import api.expenses.expenses.records.accounts.AccountRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface AccountMapper {

    AccountRecord toRecord(Account account);
    List<AccountRecord> toRecord(List<Account> account);
}
