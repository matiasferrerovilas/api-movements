package api.m2.movements.repositories;

import api.m2.movements.entities.commons.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {

    @Query("SELECT r FROM CategoryRule r JOIN FETCH r.category WHERE r.workspaceId = :workspaceId")
    List<CategoryRule> findByWorkspaceIdWithCategory(@Param("workspaceId") Long workspaceId);

    Optional<CategoryRule> findByWorkspaceIdAndMerchantKey(Long workspaceId, String merchantKey);

    boolean existsByWorkspaceId(Long workspaceId);
}
