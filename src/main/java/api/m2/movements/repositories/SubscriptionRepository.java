package api.m2.movements.repositories;

import api.m2.movements.entities.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Query(value = """
       SELECT DISTINCT s
       FROM Subscription s
       JOIN FETCH s.currency c
       JOIN FETCH s.workspace w
       LEFT JOIN FETCH s.owner
       WHERE w.id = :workspaceId
         AND (:symbols IS NULL OR c.symbol IN :symbols)
         AND (:lastPayment IS NULL OR s.lastPayment = :lastPayment)
""")
    List<Subscription> findByWorkspaceAndCurrencyAndLastPayment(
            @Param("workspaceId") Long workspaceId,
            @Param("symbols") List<String> symbols,
            @Param("lastPayment") LocalDate lastPayment
    );

    @Query(value = """
    SELECT s
    FROM Subscription s
    JOIN FETCH s.currency c
    WHERE s.id = :id
""")
    Optional<Subscription> findByIdWithCurrency(Long id);
}

