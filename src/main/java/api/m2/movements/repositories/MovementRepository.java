package api.m2.movements.repositories;

import api.m2.movements.entities.Movement;
import api.m2.movements.projections.MonthlyEvolutionProjection;
import api.m2.movements.records.balance.BalanceByCategoryRecord;
import api.m2.movements.records.balance.BalanceByGroup;
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
            INNER JOIN users u ON u.email = :email AND m.user_id = u.id
            WHERE (m.date >= :startDate)
                  AND (m.date  <= :endDate)
                  AND m.type IN (:type)
                  AND m.currency_id IN (:currencies)
                   AND m.workspace_id IN (:groups)
            """, nativeQuery = true)
    BigDecimal getBalanceByFilters(LocalDate startDate, LocalDate endDate,
                                   String email,
                                   List<String> type,
                                   List<Integer> groups,
                                   List<Integer> currencies);

    @Query("""
    SELECT g
    FROM Movement g
    WHERE
    (:#{#accountsId} IS NULL OR g.workspace.id IN :#{#accountsId})
      AND (:#{#filter.currency} IS NULL OR g.currency.symbol IN :#{#filter.currency})
      AND (:#{#filter.bank} IS NULL OR g.bank.description IN :#{#filter.bank})
      AND (:#{#filter.type} IS NULL OR g.type IN :#{#filter.type})
      AND (:#{#filter.categories} IS NULL OR g.category.description IN :#{#filter.categories})
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
                    YEAR(g.`date`) as year,
                    MONTH(g.`date`) as month,
                    c.symbol AS currencySymbol,
                    SUM(g.amount) AS total
                        FROM movements g
                        INNER JOIN currency c ON g.currency_id = c.id
                        INNER JOIN category ca ON g.category_id = ca.id
                        INNER JOIN users u ON u.email = :email AND g.user_id = u.id
                            WHERE YEAR(g.`date`) = :year AND MONTH(g.`date`) = :month
                                  AND g.type !="INGRESO"
                                   AND g.workspace_id IN (:groups)
                              AND c.symbol IN (:currencies)
                        GROUP BY ca.description, YEAR(g.`date`), c.symbol, MONTH(g.`date`), g.workspace_id
    """, nativeQuery = true)
    Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(Integer year,
                                                              Integer month,
                                                              List<Integer> groups,
                                                              List<String> currencies,
                                                              String email);



    @Query(value = """
            SELECT   ac.name AS workspaceDescription,
                    c.symbol AS currencySymbol,
                    YEAR(g.`date`) AS year,
                    MONTH(g.`date`) AS month,
                    SUM(g.amount) AS total
            from movements g
            INNER JOIN currency c ON g.currency_id = c.id
            INNER JOIN users u ON g.user_id = u.id
            INNER JOIN workspaces ac on ac.id = g.workspace_id
            WHERE YEAR(g.`date`) = :year and MONTH(g.`date`) = :month AND u.email = :email
            GROUP BY ac.name, YEAR(g.`date`), c.symbol, MONTH(g.`date`)
            ORDER BY ac.name, YEAR(g.`date`)
    """, nativeQuery = true)
    Set<BalanceByGroup> getBalanceByYearAndGroup(Integer year, Integer month, String email);

    @Query("""
    SELECT m FROM Movement m
    WHERE m.description = :description
      AND m.workspace.id = :workspaceId
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
           SUM(m.amount)   AS total
    FROM Movement m
    JOIN m.currency c
    WHERE YEAR(m.date) = :year
      AND m.type IN (
            api.m2.movements.enums.MovementType.DEBITO,
            api.m2.movements.enums.MovementType.CREDITO
          )
      AND (:#{#workspaceIds == null || #workspaceIds.isEmpty()} = true
           OR m.workspace.id IN :workspaceIds)
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
            INNER JOIN users u ON u.email = :email AND m.user_id = u.id
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type = :type
              AND c.symbol = :currency
            """, nativeQuery = true)
    BigDecimal getTotalByTypeAndMonth(String email, Integer year, Integer month, String type, String currency);

    @Query(value = """
            SELECT COALESCE(SUM(m.amount / m.exchange_rate), 0)
            FROM movements m
            INNER JOIN users u ON u.email = :email AND m.user_id = u.id
            WHERE YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type = :type
              AND m.exchange_rate IS NOT NULL
            """, nativeQuery = true)
    BigDecimal getTotalInUsdByTypeAndMonth(String email, Integer year, Integer month, String type);

    @Query(value = """
            SELECT ca.description
            FROM movements m
            INNER JOIN users u ON u.email = :email AND m.user_id = u.id
            INNER JOIN category ca ON m.category_id = ca.id
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE YEAR(m.date) = :year
              AND MONTH(m.date) = :month
              AND m.type IN ('DEBITO', 'CREDITO')
              AND c.symbol = :currency
            GROUP BY ca.description
            ORDER BY SUM(m.amount) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> getTopCategoryByMonth(String email, Integer year, Integer month, String currency);

    @Query(value = """
            SELECT DISTINCT c.symbol
            FROM movements m
            INNER JOIN users u ON u.email = :email AND m.user_id = u.id
            INNER JOIN currency c ON m.currency_id = c.id
            WHERE (YEAR(m.date) = :year AND MONTH(m.date) = :month)
               OR (YEAR(m.date) = :prevYear AND MONTH(m.date) = :prevMonth)
            """, nativeQuery = true)
    List<String> findDistinctCurrenciesByMonth(String email, Integer year, Integer month,
                                               Integer prevYear, Integer prevMonth);

    List<Movement> findByOwnerIdAndCategoryId(Long ownerId, Long categoryId);

    List<Movement> findByWorkspaceIdAndCategoryId(Long workspaceId, Long categoryId);

    List<Movement> findAllByExchangeRateIsNull();
}