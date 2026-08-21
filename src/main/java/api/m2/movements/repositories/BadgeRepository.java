package api.m2.movements.repositories;

import api.m2.movements.entities.Badge;
import api.m2.movements.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    @Query("""
            SELECT b FROM Badge b
            LEFT JOIN FETCH b.category
            WHERE b.workspaceId = :workspaceId
            ORDER BY b.earnedAt DESC
            """)
    List<Badge> findByWorkspaceIdOrderByEarnedAtDesc(@Param("workspaceId") Long workspaceId);

    boolean existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(
            Long workspaceId, Long categoryId, Integer year, Integer month, BadgeType type);
}
