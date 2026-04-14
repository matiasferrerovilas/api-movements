package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.CategoryMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
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

    public String getDefaultCategory() {
        return DefaultCategory.SIN_CATEGORIA.getDescription();
    }

    public CategoryRecord resolveDefaultCategory(String description) {
        if (StringUtils.isBlank(description)) {
            return this.findCategoryByDescription(DefaultCategory.SIN_CATEGORIA.getDescription());
        }

        String desc = description.toLowerCase();

        if (Stream.of("netflix", "hbo", "disney+").anyMatch(desc::contains)) {
            return this.findCategoryByDescription(DefaultCategory.STREAMING.getDescription());
        } else if (desc.contains("spotify")) {
            return this.findCategoryByDescription(DefaultCategory.SERVICIOS.getDescription());
        } else {
            return this.findCategoryByDescription(DefaultCategory.SIN_CATEGORIA.getDescription());
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }
}
