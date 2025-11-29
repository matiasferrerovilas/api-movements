package api.expenses.expenses.services.category;

import api.expenses.expenses.entities.Category;
import api.expenses.expenses.enums.CategoryEnum;
import api.expenses.expenses.mappers.CategoryMapper;
import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.repositories.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CategoryAddService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public Category addCategory(String description) {
        final String normalized = this.normalize(description);

        return categoryRepository.findByDescription(normalized)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .description(normalized)
                        .build()));
    }

    public CategoryRecord findCategoryByDescription(final String description) {
        return categoryMapper.toRecord(
                categoryRepository.findByDescription(this.normalize(description))
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Category not found: " + description))
        );
    }

    public List<CategoryRecord> getAllCategories() {
        return categoryMapper.toRecordList(categoryRepository.findAll());
    }

    public String getDefaultCategory() {
        return CategoryEnum.SIN_CATEGORIA.getDescripcion();
    }

    public CategoryRecord getCategoryAtLoadDefaultByStringHelper(String description) {
        if (StringUtils.isBlank(description)) {
            return this.findCategoryByDescription(CategoryEnum.SIN_CATEGORIA.getDescripcion());
        }

        String desc = description.toLowerCase();

        if (Stream.of("netflix", "hbo", "disney+").anyMatch(desc.toLowerCase()::contains)) {
            return this.findCategoryByDescription(CategoryEnum.STREAMING.getDescripcion());
        } else if (desc.contains("spotify")) {
            return this.findCategoryByDescription(CategoryEnum.SERVICIOS.getDescripcion());
        } else {
            return this.findCategoryByDescription(CategoryEnum.SIN_CATEGORIA.getDescripcion());
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }
}