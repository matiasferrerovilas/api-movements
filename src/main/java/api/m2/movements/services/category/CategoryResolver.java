package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.records.categories.CategoryRecord;
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