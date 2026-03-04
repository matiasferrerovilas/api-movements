package api.m2.movements.repositories;

import api.m2.movements.entities.AccountMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountMemberRepository extends JpaRepository<AccountMember, Long> {

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
    SELECT m
    FROM AccountMember m
    WHERE m.user.id = :userId
      AND m.isDefault = true
""")
    Optional<AccountMember> findCurrentDefault(@Param("userId") Long userId);
}
