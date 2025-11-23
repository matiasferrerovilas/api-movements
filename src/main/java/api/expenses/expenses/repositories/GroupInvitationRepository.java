package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.GroupInvitation;
import api.expenses.expenses.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    List<GroupInvitation> findAllByGroupIdAndStatus(Long groupId, InvitationStatus status);

    @Query("""
       SELECT gi
       FROM GroupInvitation gi
       JOIN FETCH gi.user u
       JOIN FETCH gi.group g
       JOIN FETCH gi.invitedBy ib
       WHERE gi.user.id = :userId
         AND gi.status = :status
       """)
    List<GroupInvitation> findAllByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") InvitationStatus status
    );
}
