package api.m2.movements.repositories;

import api.m2.movements.entities.investments.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    @Query("""
            SELECT i FROM Investment i
            JOIN FETCH i.currency
            JOIN FETCH i.investmentType
            JOIN FETCH i.workspace
            LEFT JOIN FETCH i.owner
            WHERE i.workspace.id = :workspaceId
            ORDER BY i.startDate DESC
            """)
    List<Investment> findByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
