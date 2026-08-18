package api.m2.movements.repositories;

import api.m2.movements.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query("""
            SELECT g FROM Goal g
            JOIN FETCH g.currency
            WHERE g.workspaceId = :workspaceId
            ORDER BY g.createdAt DESC
            """)
    List<Goal> findByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
