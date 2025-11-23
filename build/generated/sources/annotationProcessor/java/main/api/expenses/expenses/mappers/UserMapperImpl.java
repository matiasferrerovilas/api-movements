package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.records.groups.UserRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-23T12:17:53-0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 25 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserRecord toRecord(User userGroups) {
        if ( userGroups == null ) {
            return null;
        }

        String email = null;
        List<UserGroupsRecord> userGroups1 = null;
        Long id = null;

        email = userGroups.getEmail();
        userGroups1 = userGroupsSetToUserGroupsRecordList( userGroups.getUserGroups() );
        id = userGroups.getId();

        UserRecord userRecord = new UserRecord( email, userGroups1, id );

        return userRecord;
    }

    @Override
    public List<UserRecord> toRecord(List<User> userGroups) {
        if ( userGroups == null ) {
            return null;
        }

        List<UserRecord> list = new ArrayList<UserRecord>( userGroups.size() );
        for ( User user : userGroups ) {
            list.add( toRecord( user ) );
        }

        return list;
    }

    protected UserGroupsRecord userGroupsToUserGroupsRecord(UserGroups userGroups) {
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

    protected List<UserGroupsRecord> userGroupsSetToUserGroupsRecordList(Set<UserGroups> set) {
        if ( set == null ) {
            return null;
        }

        List<UserGroupsRecord> list = new ArrayList<UserGroupsRecord>( set.size() );
        for ( UserGroups userGroups : set ) {
            list.add( userGroupsToUserGroupsRecord( userGroups ) );
        }

        return list;
    }
}
