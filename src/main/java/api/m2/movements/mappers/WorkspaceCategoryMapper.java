package api.m2.movements.mappers;

import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.records.categories.CategoryRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkspaceCategoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", source = "category.description")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isDeletable", source = "category.deletable")
    @Mapping(target = "iconName", source = "iconName")
    @Mapping(target = "iconColor", source = "iconColor")
    CategoryRecord toRecord(WorkspaceCategory workspaceCategory);

    List<CategoryRecord> toRecordList(List<WorkspaceCategory> workspaceCategories);
}
