package api.m2.movements.repositories;

import api.m2.movements.entities.movements.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    // countQuery explícito porque la derivación automática de Spring Data no maneja bien el
    // DISTINCT + JOIN FETCH de la query principal (contaría de más o fallaría al generar el count).
    @Query(value = """
        SELECT DISTINCT i
        FROM Income i
        JOIN FETCH i.currency
        JOIN FETCH i.bank b
        WHERE i.workspaceId = :workspaceId
""",
        countQuery = """
        SELECT COUNT(DISTINCT i)
        FROM Income i
        WHERE i.workspaceId = :workspaceId
""")
    Page<Income> findAllByWorkspaceId(Long workspaceId, Pageable pageable);

    @Query("""
        SELECT i
        FROM Income i
        JOIN FETCH i.currency
        JOIN FETCH i.bank
        WHERE i.userId = :userId
    """)
    List<Income> findAllByUserId(Long userId);
}
