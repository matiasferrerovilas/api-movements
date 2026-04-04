package api.m2.movements.mappers;

import api.m2.movements.entities.UserCategory;
import api.m2.movements.records.categories.CategoryRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCategoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", source = "category.description")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isDeletable", source = "category.deletable")
    CategoryRecord toRecord(UserCategory userCategory);

    List<CategoryRecord> toRecordList(List<UserCategory> userCategories);
}
