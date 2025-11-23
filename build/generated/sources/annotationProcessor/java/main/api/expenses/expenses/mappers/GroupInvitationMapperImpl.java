package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.GroupInvitation;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
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
public class GroupInvitationMapperImpl implements GroupInvitationMapper {

    @Override
    public List<GroupInvitationRecord> toRecord(List<GroupInvitation> groupInvitations) {
        if ( groupInvitations == null ) {
            return null;
        }

        List<GroupInvitationRecord> list = new ArrayList<GroupInvitationRecord>( groupInvitations.size() );
        for ( GroupInvitation groupInvitation : groupInvitations ) {
            list.add( groupInvitationToGroupInvitationRecord( groupInvitation ) );
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

    protected GroupInvitationRecord groupInvitationToGroupInvitationRecord(GroupInvitation groupInvitation) {
        if ( groupInvitation == null ) {
            return null;
        }

        Long id = null;
        UserGroupsRecord group = null;

        id = groupInvitation.getId();
        group = userGroupsToUserGroupsRecord( groupInvitation.getGroup() );

        GroupInvitationRecord groupInvitationRecord = new GroupInvitationRecord( id, group );

        return groupInvitationRecord;
    }
}
