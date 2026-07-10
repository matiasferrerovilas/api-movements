package api.m2.movements.movements.repositories;

import api.m2.movements.movements.entities.MonthlySummarySnapshot;
import api.m2.movements.identity.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlySummarySnapshotRepository extends JpaRepository<MonthlySummarySnapshot, Long> {

    Optional<MonthlySummarySnapshot> findByUserAndYearAndMonth(User user, Integer year, Integer month);
}
