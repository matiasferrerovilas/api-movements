package api.m2.movements.investment.repositories;

import api.m2.movements.investment.entities.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentTypeRepository extends JpaRepository<InvestmentType, Long> {

    List<InvestmentType> findByWorkspaceId(Long workspaceId);
}
