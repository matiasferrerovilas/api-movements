package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Services, Long> {
    @Query(value = """
      select distinct s
        from Services s
        join fetch s.currency c
        join fetch s.account a
        left join fetch s.owner
        join a.members m
        where m.user.id = :userId
          and (:symbols is null or c.symbol in :symbols)
          and (:lastPayment is null or s.lastPayment = :lastPayment)
""")
    List<Services> findByCurrencyAndLastPayment(
            @Param("userId") Long userId,
            @Param("symbols") List<String> symbols,
            @Param("lastPayment") LocalDate lastPayment
    );

    @Query(value = """
    SELECT s
    FROM Services s
    JOIN FETCH s.currency c
    WHERE s.id = :id
""")
    Optional<Services> findByIdWithCurrency(Long id);
}
