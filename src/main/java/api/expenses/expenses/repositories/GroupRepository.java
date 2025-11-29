package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.records.groups.GroupsWIthUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<UserGroups, Long> {
    Optional<UserGroups> findByDescription(String group);

    @Query(value = """

            SELECT
            g.id,
            g.description,
            COUNT(DISTINCT uu.user_id) AS member_count
            FROM user_groups g
            JOIN user_user_groups uug ON uug.group_id = g.id
            JOIN user_user_groups uu ON uu.group_id = g.id
            WHERE uug.user_id = :userId and g.description != "DEFAULT"
            GROUP BY g.id, g.description
            ORDER BY g.id
        """, nativeQuery = true)
    List<GroupsWIthUser> findGroupsByUserIdWithMemberCount(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = """
    DELETE FROM user_user_groups
    WHERE group_id = :groupId AND user_id = :userId
""", nativeQuery = true)
    void deleteUserFromGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query(value = """
    SELECT COUNT(*)
    FROM user_groups ug
    INNER JOIN user_user_groups uug on uug.group_id  = ug.id
    WHERE uug.user_id = :userId AND uug.group_id = :groupId AND ug.description != "DEFAULT";
""", nativeQuery = true)
    int userBelongsToGroup(Long userId, Long groupId);

    @Query(value = """
    SELECT g.*
    FROM user_groups g
    INNER JOIN user_user_groups uug ON uug.group_id = g.id
    WHERE uug.user_id = :userId
""", nativeQuery = true)
    List<UserGroups> findGroupsOfUser(@Param("userId") Long userId);

}
