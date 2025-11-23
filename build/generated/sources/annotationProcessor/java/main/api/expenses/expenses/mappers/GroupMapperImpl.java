package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.records.groups.UserGroupsRecord;
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
public class GroupMapperImpl implements GroupMapper {

    @Override
    public UserGroupsRecord toRecord(UserGroups userGroups) {
        if ( userGroups == null ) {
            return null;
        }

        String description = null;
        Long id = null;

        description = userGroups.getDescription();
        id = userGroups.getId();

        UserGroupsRecord userGroupsRecord = new UserGroupsRecord( description, id );

        return userGroupsRecord;
    }

    @Override
    public List<UserGroupsRecord> toRecord(List<UserGroups> userGroups) {
        if ( userGroups == null ) {
            return null;
        }

        List<UserGroupsRecord> list = new ArrayList<UserGroupsRecord>( userGroups.size() );
        for ( UserGroups userGroups1 : userGroups ) {
            list.add( toRecord( userGroups1 ) );
        }

        return list;
    }
}
