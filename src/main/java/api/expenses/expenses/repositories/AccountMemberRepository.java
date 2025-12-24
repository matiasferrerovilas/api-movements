package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.AccountMember;
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
        WHERE a.id = :accountId
          AND m.user.id = :userId
    """)
    Optional<AccountMember> findMember(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId
    );
}
