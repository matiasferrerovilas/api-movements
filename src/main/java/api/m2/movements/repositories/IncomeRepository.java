package api.m2.movements.repositories;

import api.m2.movements.entities.movements.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    @Query(value = """
        SELECT DISTINCT i
        FROM Income i
        JOIN FETCH i.currency
        JOIN FETCH i.bank b
        WHERE i.workspaceId = :workspaceId
""")
    List<Income> findAllByWorkspaceId(Long workspaceId);

    @Query("""
        SELECT i
        FROM Income i
        JOIN FETCH i.currency
        JOIN FETCH i.bank
        WHERE i.userId = :userId
    """)
    List<Income> findAllByUserId(Long userId);
}
