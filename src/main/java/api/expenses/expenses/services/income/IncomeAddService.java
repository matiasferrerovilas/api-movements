package api.expenses.expenses.services.income;

import api.expenses.expenses.records.income.IngresoToAdd;
import api.expenses.expenses.repositories.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeAddService {
    private final IncomeRepository incomeRepository;

    public void loadIncome(List<IngresoToAdd> ingresoToAdds) {
    }
}
