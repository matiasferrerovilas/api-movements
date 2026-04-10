package api.m2.movements.repositories;

import api.m2.movements.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("""
            SELECT b FROM Budget b
            JOIN FETCH b.category
            JOIN FETCH b.currency
            WHERE b.account.id = :accountId
              AND b.currency.symbol = :currencySymbol
              AND (b.year IS NULL OR (b.year = :year AND b.month = :month))
            """)
    List<Budget> findByAccountAndPeriod(
            @Param("accountId") Long accountId,
            @Param("currencySymbol") String currencySymbol,
            @Param("year") int year,
            @Param("month") int month
    );

    Optional<Budget> findByAccountIdAndCategoryIdAndCurrencyId(
            Long accountId, Long categoryId, Long currencyId
    );

    @Query(value = """
            SELECT COALESCE(SUM(m.amount), 0)
            FROM movements m
            INNER JOIN category ca ON m.category_id = ca.id
            INNER JOIN currency c  ON m.currency_id  = c.id
            WHERE m.account_id   = :accountId
              AND ca.description = :categoryDescription
              AND c.symbol       = :currencySymbol
              AND YEAR(m.date)   = :year
              AND MONTH(m.date)  = :month
              AND m.type        != 'INGRESO'
            """, nativeQuery = true)
    BigDecimal sumSpentByCategoryAndPeriod(
            @Param("accountId") Long accountId,
            @Param("categoryDescription") String categoryDescription,
            @Param("currencySymbol") String currencySymbol,
            @Param("year") int year,
            @Param("month") int month
    );
}
