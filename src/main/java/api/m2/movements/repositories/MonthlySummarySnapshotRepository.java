package api.m2.movements.repositories;

import api.m2.movements.entities.MonthlySummarySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlySummarySnapshotRepository extends JpaRepository<MonthlySummarySnapshot, Long> {

    Optional<MonthlySummarySnapshot> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
}
