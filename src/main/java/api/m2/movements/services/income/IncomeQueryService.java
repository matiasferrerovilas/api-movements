package api.m2.movements.services.income;

import api.m2.movements.mappers.IncomeMapper;
import api.m2.movements.records.income.IncomeRecord;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeQueryService {
    private final IncomeRepository incomeRepository;
    private final UserService userService;
    private final IncomeMapper incomeMapper;

    public List<IncomeRecord> getAllIncomes() {
        var user = userService.getAuthenticatedUser();
        return incomeMapper.toRecord(incomeRepository.findAllByUserOrGroupsIn(user.getId()));
    }
}
