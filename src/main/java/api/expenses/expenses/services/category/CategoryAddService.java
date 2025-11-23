package api.expenses.expenses.services.category;

import api.expenses.expenses.entities.Category;
import api.expenses.expenses.enums.CategoryEnum;
import api.expenses.expenses.mappers.CategoryMapper;
import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryAddService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public Category addCategory(String category){
        return categoryRepository.findByDescription(category)
                .orElseGet(() -> categoryRepository.save(Category.builder().description(category).build()));
    }

    public CategoryRecord findCategoryByDescription(String category){
        return categoryMapper.toRecord(this.addCategory(category));
    }

    public List<CategoryRecord> getAllCategories() {
        return categoryMapper.toRecordList(categoryRepository.findAll());
    }

    public String getDefaultCategory() {
        return categoryRepository.findById(1L).orElseThrow().getDescription();
    }

    public CategoryRecord getCategoryAtLoadDefaultByStringHelper(String description) {
        if (StringUtils.isBlank(description)) {
            return this.findCategoryByDescription(CategoryEnum.SIN_CATEGORIA.getDescripcion());
        }

        String desc = description.toLowerCase();

        if (StringUtils.containsAny(desc, "netflix", "hbo", "disney+")) {
            return this.findCategoryByDescription(CategoryEnum.STREAMING.getDescripcion());
        } else if (StringUtils.contains(desc, "spotify")) {
            return this.findCategoryByDescription(CategoryEnum.SERVICIOS.getDescripcion());
        } else {
            return this.findCategoryByDescription(CategoryEnum.SIN_CATEGORIA.getDescripcion());
        }
    }
}
