package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.records.balance.BalanceByCategoryRecord;
import api.expenses.expenses.records.balance.BalanceByGroup;
import api.expenses.expenses.records.movements.MovementSearchFilterRecord;
import api.expenses.expenses.records.groups.UserRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MovementRepository extends JpaRepository<Movement, Long> {
    @Query(value = """
            SELECT COALESCE(SUM(m.amount), 0)
            FROM movements m
            INNER JOIN user_groups ug on ug.id = m.user_group_id
            INNER JOIN users u ON u.email = :email
            WHERE (:year IS NULL OR m.year = :year)
                          AND (:month IS NULL OR m.month = :month)
                          AND m.type IN (:type)
                          AND m.currency_id IN (:currencies)
                          AND ug.id IN (:groups)
                          AND((m.user_group_id = 1 AND m.user_id = u.id)
                                              OR
                                          (m.user_group_id != 1 AND EXISTS (
                                                      SELECT 1
                                                                  FROM user_user_groups uug
                                                                              WHERE uug.user_id = u.id
                                                                                            AND uug.group_id = m.user_group_id
                                                                                          ))
                                          )
            """, nativeQuery = true)
    BigDecimal getBalanceByFilters(Integer year, Integer month,
                                   String email,
                                   List<String> type,
                                   List<Integer> groups,
                                   List<Integer> currencies);

    @Query("""
    SELECT g
    FROM Movement g
    WHERE
    (:#{#accountsId} IS NULL OR g.account.id IN :#{#accountsId})
      AND (:#{#filter.currency} IS NULL OR g.currency.symbol IN :#{#filter.currency})
      AND (:#{#filter.bank} IS NULL OR g.bank IN :#{#filter.bank})
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
                                INNER JOIN users u ON u.email = :email
                            WHERE YEAR(g.`date`) = :year AND MONTH(g.`date`) = :month
                                  AND g.type !="INGRESO"
                                  AND g.user_group_id IN (:groups)
                              AND c.symbol IN (:currencies)
                              AND ((g.user_group_id = 1 AND g.user_id = u.id)
                                           OR
                                       (g.user_group_id != 1 AND EXISTS (
                                           SELECT 1
                                            FROM user_user_groups uug
                                            WHERE uug.user_id = u.id
                                                  AND uug.group_id = g.user_group_id
                                                )
                                        )
                                      )
                        GROUP BY ca.description, YEAR(g.`date`), c.symbol, MONTH(g.`date`), g.user_group_id
    """, nativeQuery = true)
    Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(Integer year,
                                                              Integer month,
                                                              List<Integer> groups,
                                                              List<String> currencies,
                                                              String email);

    @Query(value = """
            SELECT g
            FROM Movement g
            JOIN fetch g.currency c
            where g.id = :id
        """)
    Optional<Movement> findByIdWithCurrency(Long id);

    //TODO REVISAR ESTO
    @Query(value = """
            SELECT g
            FROM Movement g
            JOIN fetch g.currency c
            WHERE (g.owner.email = :#{#user.email})
            ORDER BY g.date DESC
            LIMIT 1
        """)
    Optional<Movement> getLastIngreso(UserRecord user);

    @Query(value = """

               SELECT
                      ug.description AS groupDescription,
                    c.symbol AS currencySymbol,
                    YEAR(g.`date`) AS year,
                    MONTH(g.`date`) AS month,
                    SUM(g.amount) AS total
                        FROM movements g
                        INNER JOIN currency c ON g.currency_id = c.id
                        INNER JOIN user_groups ug on ug.id = g.user_group_id
                        INNER JOIN users u ON g.user_id = u.id
                        WHERE YEAR(g.`date`) = :year and MONTH(g.`date`) = :month
                  AND u.email = :email
                        GROUP BY ug.description, YEAR(g.`date`), c.symbol, MONTH(g.`date`)
                        ORDER BY ug.description, YEAR(g.`date`)
    """, nativeQuery = true)
    Set<BalanceByGroup> getBalanceByYearAndGroup(Integer year, Integer month, String email);
}