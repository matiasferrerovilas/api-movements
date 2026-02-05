package api.m2.movements.mappers;

import api.m2.movements.entities.Category;
import api.m2.movements.records.categories.CategoryRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    Category toEntity(String category);
    List<CategoryRecord> toRecordList(List<Category> all);

    CategoryRecord toRecord(Category category);
}
