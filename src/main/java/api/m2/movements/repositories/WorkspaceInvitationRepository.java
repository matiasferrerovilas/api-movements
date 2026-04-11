package api.m2.movements.repositories;

import api.m2.movements.entities.WorkspaceInvitation;
import api.m2.movements.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    List<WorkspaceInvitation> findAllByWorkspaceIdAndStatus(Long workspaceId, InvitationStatus status);

    @Query("""
       SELECT gi
       FROM WorkspaceInvitation gi
       LEFT JOIN FETCH gi.user u
       JOIN FETCH gi.workspace g
       JOIN FETCH gi.invitedBy ib
       WHERE gi.user.id = :userId
         AND gi.status = :status
       """)
    List<WorkspaceInvitation> findAllByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") InvitationStatus status
    );
}
