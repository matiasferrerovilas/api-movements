package api.m2.movements.mappers;

import api.m2.movements.entities.Category;
import api.m2.movements.records.categories.CategoryRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    Category toEntity(String category);

    CategoryRecord toRecord(Category category);
}
