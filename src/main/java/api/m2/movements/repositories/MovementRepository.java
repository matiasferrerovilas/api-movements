package api.m2.movements.repositories;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.projections.MonthlyEvolutionProjection;
import api.m2.movements.records.balance.BalanceByCategoryRecord;
import api.m2.movements.records.balance.CategoryAmountRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MovementRepository extends JpaRepository<Movement, Long> {
    @Query(value = """
            SELECT COALESCE(SUM(m.amount), 0)
            FROM movements m
            WHERE m.user_id = :userId
                  AND (m.date >= :startDate)
                  AND (m.date  <= :endDate)
                  AND m.type IN (:type)
                  AND m.currency_id IN (:currencies)
                   AND m.workspace_id IN (:groups)
            """, nativeQuery = true)
    BigDecimal getBalanceByFilters(LocalDate startDate, LocalDate endDate,
                                   Long userId,
                                   List<String> type,
                                   List<Integer> groups,
                                   List<Integer> currencies);

    @Query("""
    SELECT g
    FROM Movement g
    WHERE
    (:#{#accountsId} IS NULL OR g.workspaceId IN :#{#accountsId})
      AND (:#{#filter.currency} IS NULL OR g.currency.symbol IN :#{#filter.currency})
      AND (:#{#filter.bank} IS NULL OR g.bank.description IN :#{#filter.bank})
      AND (:#{#filter.type} IS NULL OR g.type IN :#{#filter.type})
      AND (:#{#filter.categories} IS NULL OR EXISTS (
          SELECT 1 FROM g.categories gc WHERE gc.description IN :#{#filter.categories}
      ))
      AND (:#{#filter.description} IS NULL OR LOWER(g.description) LIKE LOWER(CONCAT('%', :#{#filter.description}, '%')))
      AND (
          (:#{#filter.isLive} = TRUE AND
              FUNCTION('MONTH', g.date) = FUNCTION('MONTH', CURRENT_DATE)
              AND FUNCTION('YEAR', g.date) = FUNCTION('YEAR', CURRENT_DATE)
          )
          OR (:#{#filter.isLive} = FALSE AND
              (:#{#filter.dateFrom} IS NULL OR g.date >= :#{#filter.dateFrom}) AND
              (:#{#filter.dateTO} IS NULL OR g.date <= :#{#filter.dateTO})
          )
          OR (:#{#filter.isLive} IS NULL)
      )
      ORDER BY g.date DESC
""")
    Page<Movement> getExpenseBy(
            @Param("accountsId") List<Long> accountsId,
            @Param("filter") MovementSearchFilterRecord filter,
            Pageable pageable
    );


    @Query(value = """
        SELECT
                    ca.description AS category,
                    CAST(YEAR(g.`date`) AS SIGNED) as year,
                    CAST(MONTH(g.`date`) AS SIGNED) as month,
                    c.symbol AS currencySymbol,
                    SUM(g.amount) AS total
                        FROM movements g
                        INNER JOIN currency c ON g.currency_id = c.id
                        INNER JOIN movement_categories mc ON mc.movement_id = g.id
                        INNER JOIN category ca ON mc.category_id = ca.id
                            WHERE YEAR(g.`date`) = :year AND MONTH(g.`date`) = :month
                                  AND g.type !="INGRESO"
                                   AND g.workspace_id IN (:groups)
                              AND c.symbol IN (:currencies)
                        GROUP BY ca.description, CAST(YEAR(g.`date`) AS SIGNED), c.symbol, CAST(MONTH(g.`date`) AS SIGNED), g.workspace_id
    """, nativeQuery = true)
    Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(Integer year,
                                                              Integer month,
                                                              List<Integer> groups,
                                                              List<String> currencies);



    @Query("""
    SELECT m FROM Movement m
    WHERE m.description = :description
      AND m.workspaceId = :workspaceId
      AND YEAR(m.date) = :year
      AND MONTH(m.date) = :month
    """)
    Optional<Movement> findByDescriptionAndAccountAndMonth(
            @Param("description") String description,
            @Param("workspaceId") Long workspaceId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
    SELECT MONTH(m.date)   AS month,
           c.symbol        AS currencySymbol,
           SUM(CASE WHEN m.type IN (
                api.m2.movements.enums.MovementType.DEBITO,
                api.m2.movements.enums.MovementType.CREDITO
              ) THEN m.amount ELSE 0 END) AS spent,
           SUM(CASE WHEN m.type = api.m2.movements.enums.MovementType.INGRESO
              THEN m.amount ELSE 0 END)   AS income
    FROM Movement m
    JOIN m.currency c
    WHERE YEAR(m.date) = :year
      AND (:#{#workspaceIds == null || #workspaceIds.isEmpty()} = true
           OR m.workspaceId IN :workspaceIds)
    GROUP BY MONTH(m.date), c.symbol
    ORDER BY MONTH(m.date)
    """)
    List<MonthlyEvolutionProjection> findMonthlyEvolution(
            @Param("year") Integer year,
            @Param("workspaceIds") List<Long> workspaceIds
    );

    @Query(value = """
            SELECT COALESCE(SUM(m.amount), 0)
            FROM movements m
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE m.workspace_id = :workspaceId
              AND YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type = :type
              AND c.symbol = :currency
            """, nativeQuery = true)
    BigDecimal getTotalByTypeAndMonth(Long workspaceId, Integer year, Integer month, String type, String currency);

    @Query(value = """
            SELECT COALESCE(SUM(m.amount / m.exchange_rate), 0)
            FROM movements m
            WHERE m.workspace_id = :workspaceId
              AND YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type = :type
              AND m.exchange_rate IS NOT NULL
            """, nativeQuery = true)
    BigDecimal getTotalInUsdByTypeAndMonth(Long workspaceId, Integer year, Integer month, String type);

    @Query(value = """
            SELECT COALESCE(SUM(m.amount / m.exchange_rate), 0)
            FROM movements m
            WHERE m.workspace_id = :workspaceId
              AND m.type = :type
              AND m.exchange_rate IS NOT NULL
            """, nativeQuery = true)
    BigDecimal getTotalInUsdByType(Long workspaceId, String type);

    @Query(value = """
            SELECT COALESCE(SUM(m.amount), 0)
            FROM movements m
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE m.workspace_id = :workspaceId
              AND m.type = :type
              AND c.symbol = :currency
            """, nativeQuery = true)
    BigDecimal getTotalByType(Long workspaceId, String type, String currency);

    @Query(value = """
            SELECT COALESCE(SUM(m.amount / m.exchange_rate), 0)
            FROM movements m
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE m.workspace_id = :workspaceId
              AND YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type = :type
              AND m.exchange_rate IS NOT NULL
              AND c.symbol <> :currency
            """, nativeQuery = true)
    BigDecimal getTotalInUsdByTypeAndMonthExcludingCurrency(Long workspaceId, Integer year, Integer month,
                                                              String type, String currency);

    @Query(value = """
            SELECT COALESCE(SUM(m.amount / m.exchange_rate), 0)
            FROM movements m
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE m.workspace_id = :workspaceId
              AND m.type = :type
              AND m.exchange_rate IS NOT NULL
              AND c.symbol <> :currency
            """, nativeQuery = true)
    BigDecimal getTotalInUsdByTypeExcludingCurrency(Long workspaceId, String type, String currency);

    @Query(value = """
            SELECT ca.description
            FROM movements m
            INNER JOIN movement_categories mc ON mc.movement_id = m.id
            INNER JOIN category ca ON mc.category_id = ca.id
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE m.workspace_id = :workspaceId
              AND YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type IN ('DEBITO', 'CREDITO')
              AND c.symbol = :currency
            GROUP BY ca.description
            ORDER BY SUM(m.amount) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> getTopCategoryByMonth(Long workspaceId, Integer year, Integer month, String currency);

    @Query(value = """
            SELECT ca.description AS category, COALESCE(SUM(m.amount), 0) AS amount
            FROM movements m
            INNER JOIN movement_categories mc ON mc.movement_id = m.id
            INNER JOIN category ca ON mc.category_id = ca.id
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE m.workspace_id = :workspaceId
              AND YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type IN ('DEBITO', 'CREDITO')
              AND c.symbol = :currency
            GROUP BY ca.description
            ORDER BY amount DESC
            """, nativeQuery = true)
    List<CategoryAmountRecord> getCategoryTotalsByMonth(Long workspaceId, Integer year, Integer month, String currency);

    boolean existsByWorkspaceId(Long workspaceId);

    @Query(value = "SELECT DISTINCT m.workspace_id FROM movements m", nativeQuery = true)
    List<Long> findDistinctWorkspaceIds();

    @Query("""
    SELECT m FROM Movement m
    JOIN m.categories c
    WHERE m.workspaceId = :workspaceId AND c.id = :categoryId
    """)
    List<Movement> findByWorkspaceIdAndCategoryId(@Param("workspaceId") Long workspaceId,
                                                    @Param("categoryId") Long categoryId);

    List<Movement> findAllByExchangeRateIsNull();
}