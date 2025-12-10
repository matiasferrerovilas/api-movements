package api.expenses.expenses.repositories;

import api.expenses.expenses.entities.Ingreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeRepository extends JpaRepository<Ingreso, Long> {
}
