package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.mappers.CategoryMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CategoryAddService {

    private static final String SIN_CATEGORIA = "SIN_CATEGORIA";
    private static final String STREAMING     = "STREAMING";
    private static final String SERVICIOS     = "SERVICIOS";

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

    public String getDefaultCategory() {
        return SIN_CATEGORIA;
    }

    public CategoryRecord getCategoryAtLoadDefaultByStringHelper(String description) {
        if (StringUtils.isBlank(description)) {
            return this.findCategoryByDescription(SIN_CATEGORIA);
        }

        String desc = description.toLowerCase();

        if (Stream.of("netflix", "hbo", "disney+").anyMatch(desc::contains)) {
            return this.findCategoryByDescription(STREAMING);
        } else if (desc.contains("spotify")) {
            return this.findCategoryByDescription(SERVICIOS);
        } else {
            return this.findCategoryByDescription(SIN_CATEGORIA);
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }
}
