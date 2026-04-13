package api.m2.movements.repositories;

import api.m2.movements.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    @Query(value = """
        select distinct i
        from Income i
        join fetch i.currency
        join fetch i.workspace a
        join fetch i.bank b
        join a.members m
        where m.user.id = :userId
""")
    List<Income> findAllByUserOrGroupsIn(Long userId);

    @Query("""
        select i
        from Income i
        join fetch i.currency
        join fetch i.workspace
        join fetch i.bank
        where i.user.id = :userId
    """)
    List<Income> findAllByUserId(Long userId);
}
