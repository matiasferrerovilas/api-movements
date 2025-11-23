package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Budget;
import api.expenses.expenses.entities.Category;
import api.expenses.expenses.records.BudgetToAdd;
import api.expenses.expenses.repositories.CategoryRepository;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface BudgetMapper {

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    Budget toEntity(BudgetToAdd budgetToAdd, @Context CategoryRepository categoryRepository);

    @Named("mapCategory")
    default Category mapCategory(String categoryName, @Context CategoryRepository categoryRepository) {
        if (categoryName == null) {
            return null;
        }
        return categoryRepository.findByDescription(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
    }
}