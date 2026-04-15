package api.m2.movements.mappers;

import api.m2.movements.entities.Category;
import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.records.categories.CategoryRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    Category toEntity(String category);

    CategoryRecord toRecord(Category category);

    /**
     * Mapea Category a CategoryRecord incluyendo iconName e iconColor desde WorkspaceCategory.
     * Si workspaceCategory es null, usa valores por defecto.
     *
     * @param category la categoría global
     * @param workspaceCategory la categoría del workspace (puede ser null)
     * @return CategoryRecord con iconos
     */
    default CategoryRecord toRecordWithIcons(Category category, WorkspaceCategory workspaceCategory) {
        if (category == null) {
            return null;
        }

        return new CategoryRecord(
                category.getId(),
                category.getDescription(),
                workspaceCategory != null && workspaceCategory.isActive(),
                category.isDeletable(),
                workspaceCategory != null ? workspaceCategory.getIconName() : "QuestionOutlined",
                workspaceCategory != null ? workspaceCategory.getIconColor() : "#d9d9d9"
        );
    }
}
