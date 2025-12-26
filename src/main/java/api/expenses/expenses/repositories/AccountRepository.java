package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.Account;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query(value = """
        SELECT a.*
        FROM accounts a
        INNER JOIN account_members am on am.account_id  = a.id
        WHERE am.user_id = :userId;
    """, nativeQuery = true)
    List<Account> findAllAccountsByMemberId(Long userId);

    @Query("""
        select distinct a
        from Account a
        join fetch a.members m
        left join fetch a.owner o
        where m.user.id = :userId
    """)
    List<Account> findAllAccountsByMemberIdWithMembers(Long userId);

    Optional<Account> findAccountByNameAndOwnerId(@NotNull String name, Long id);
}
