package api.m2.movements.repositories;

import api.m2.movements.entities.AccountInvitation;
import api.m2.movements.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountInvitationRepository extends JpaRepository<AccountInvitation, Long> {
    List<AccountInvitation> findAllByAccountIdAndStatus(Long groupId, InvitationStatus status);

    @Query("""
       SELECT gi
       FROM AccountInvitation gi
       LEFt JOIN FETCH gi.user u
       JOIN FETCH gi.account g
       JOIN FETCH gi.invitedBy ib
       WHERE gi.user.id = :userId
         AND gi.status = :status
       """)
    List<AccountInvitation> findAllByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") InvitationStatus status
    );
}
