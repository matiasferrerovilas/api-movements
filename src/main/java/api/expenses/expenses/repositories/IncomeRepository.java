package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    @Query(value = """
            SELECT i
            FROM Income i
            JOIN FETCH i.currency c
            JOIN FETCH i.userGroups ug
            WHERE i.id =:userId OR ug.id IN (:groups)
""")
    List<Income> findAllByUserOrGroupsIn(Long userId, List<Long> groups);
}
