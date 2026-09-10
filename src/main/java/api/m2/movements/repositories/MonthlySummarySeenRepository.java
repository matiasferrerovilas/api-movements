package api.m2.movements.repositories;

import api.m2.movements.entities.MonthlySummarySeen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlySummarySeenRepository extends JpaRepository<MonthlySummarySeen, Long> {

    boolean existsByWorkspaceIdAndUserIdAndYearAndMonth(Long workspaceId, Long userId, Integer year, Integer month);
}
