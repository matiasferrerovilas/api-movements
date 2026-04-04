package api.m2.movements.repositories;

import api.m2.movements.entities.AccountMember;
import api.m2.movements.projections.MembershipSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<AccountMember, Long> {

    @Query("""
        SELECT m
        FROM Account a
        JOIN a.members m
        WHERE a.id = :groupId
          AND m.user.id = :userId
    """)
    Optional<AccountMember> findMember(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId
    );

    @Query("""
    SELECT
        m.account.id as accountId,
        m.id as membershipId,
        m.account.name as groupDescription,
        m.role as role
    FROM AccountMember m
    WHERE m.user.id = :userId
      AND m.account.isActive = true
""")
    List<MembershipSummaryProjection> findAllByUserId(Long userId);

    @Query("SELECT COUNT(m) FROM AccountMember m WHERE m.account.id = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);
}