package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Budget;
import api.expenses.expenses.records.BudgetToAdd;
import api.expenses.expenses.repositories.CategoryRepository;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-23T12:17:53-0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 25 (Oracle Corporation)"
)
@Component
public class BudgetMapperImpl implements BudgetMapper {

    @Override
    public Budget toEntity(BudgetToAdd budgetToAdd, CategoryRepository categoryRepository) {
        if ( budgetToAdd == null ) {
            return null;
        }

        Budget budget = new Budget();

        budget.setCategory( mapCategory( budgetToAdd.category(), categoryRepository ) );
        budget.setAmount( budgetToAdd.amount() );
        budget.setYear( budgetToAdd.year() );
        budget.setMonth( budgetToAdd.month() );

        return budget;
    }
}
