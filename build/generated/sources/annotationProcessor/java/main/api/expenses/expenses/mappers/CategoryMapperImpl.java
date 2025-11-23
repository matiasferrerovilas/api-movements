package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Category;
import api.expenses.expenses.records.categories.CategoryRecord;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-23T12:17:53-0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 25 (Oracle Corporation)"
)
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public Category toEntity(String category) {
        if ( category == null ) {
            return null;
        }

        Category.CategoryBuilder category1 = Category.builder();

        return category1.build();
    }

    @Override
    public List<CategoryRecord> toRecordList(List<Category> all) {
        if ( all == null ) {
            return null;
        }

        List<CategoryRecord> list = new ArrayList<CategoryRecord>( all.size() );
        for ( Category category : all ) {
            list.add( toRecord( category ) );
        }

        return list;
    }

    @Override
    public CategoryRecord toRecord(Category category) {
        if ( category == null ) {
            return null;
        }

        String description = null;

        description = category.getDescription();

        CategoryRecord categoryRecord = new CategoryRecord( description );

        return categoryRecord;
    }
}
