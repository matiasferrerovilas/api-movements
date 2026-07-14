package api.m2.movements.investment.repositories;

import api.m2.movements.investment.entities.Investment;
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
            WHERE i.workspaceId = :workspaceId
            ORDER BY i.startDate DESC
            """)
    List<Investment> findByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
