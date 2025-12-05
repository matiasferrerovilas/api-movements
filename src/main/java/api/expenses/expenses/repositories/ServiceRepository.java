package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.Services;
import api.expenses.expenses.records.groups.UserRecord;
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
    SELECT s
    FROM Services s
    JOIN FETCH s.currency c
    LEFT JOIN FETCH s.users u
    LEFT JOIN FETCH s.userGroups ug
    WHERE (:symbols IS NULL OR c.symbol IN :symbols)
     AND (:lastPayment IS NULL OR s.lastPayment = :lastPayment)
     AND (
        (s.userGroups.description = 'DEFAULT' AND s.users.email = :#{#user.email})
        OR (s.userGroups.description <> 'DEFAULT' AND s.userGroups.description IN :#{#user.userGroups.![description]})
    )
""")
    List<Services> findByCurrencyAndLastPayment(@Param("user") UserRecord user, List<String> symbols, LocalDate lastPayment);

    @Query(value = """
    SELECT s
    FROM Services s
    JOIN FETCH s.currency c
    WHERE s.id = :id
""")
    Optional<Services> findByIdWithCurrency(Long id);
}
