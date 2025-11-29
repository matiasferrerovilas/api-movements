package api.expenses.expenses.services.category;

import api.expenses.expenses.entities.Category;
import api.expenses.expenses.records.categories.CategoryRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryResolver {
    private final CategoryAddService categoryAddService;

    public Category resolve(CategoryRecord record) {
        if (record == null) return categoryAddService.addCategory(categoryAddService.getDefaultCategory());

        return categoryAddService.addCategory(record.description());
    }

    public Category resolve(String description) {
        return categoryAddService.addCategory(description);
    }
}