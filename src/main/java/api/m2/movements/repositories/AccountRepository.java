package api.m2.movements.repositories;

import api.m2.movements.entities.Account;
import api.m2.movements.projections.AccountSummaryProjection;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    @Query("""
    select distinct a
    from Account a
    join fetch a.members
    left join fetch a.owner
    where exists (
        select 1
        from AccountMember m
        where m.account = a
          and m.user.id = :userId
    )
""")
    List<Account> findAllAccountsByMemberIdWithAllMembers(Long userId);

    Optional<Account> findAccountByNameAndOwnerId(@NotNull String name, Long id);

    @Query("""
    select
        a.id as accountId,
        a.name as accountName,
        o.id as ownerId,
        o.email as ownerEmail,
        count(m.id) as membersCount
    from Account a
    join a.members m
    join a.owner o
    where exists (
        select 1
        from AccountMember am
        where am.account = a
          and am.user.id = :userId
    )
    group by a.id, a.name, o.id, o.email
""")
    List<AccountSummaryProjection> findAccountSummariesByMemberUserId(Long userId);
}
