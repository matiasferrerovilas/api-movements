package api.m2.movements.mappers;

import api.m2.movements.entities.Budget;
import api.m2.movements.entities.Category;
import api.m2.movements.records.BudgetToAdd;
import api.m2.movements.repositories.CategoryRepository;
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