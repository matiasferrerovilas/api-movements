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
      select distinct s
         from Subscription s
        join fetch s.currency c
        join fetch s.workspace a
        left join fetch s.owner
        join a.members m
        where m.user.id = :userId
          and (:symbols is null or c.symbol in :symbols)
          and (:lastPayment is null or s.lastPayment = :lastPayment)
""")
    List<Subscription> findByCurrencyAndLastPayment(
            @Param("userId") Long userId,
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

