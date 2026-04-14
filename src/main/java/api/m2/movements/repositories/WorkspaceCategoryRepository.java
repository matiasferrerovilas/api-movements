package api.m2.movements.repositories;

import api.m2.movements.entities.WorkspaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceCategoryRepository extends JpaRepository<WorkspaceCategory, Long> {

    @Query("SELECT wc FROM WorkspaceCategory wc "
            + "JOIN FETCH wc.category "
            + "WHERE wc.workspace.id = :workspaceId AND wc.isActive = true")
    List<WorkspaceCategory> findByWorkspaceIdAndIsActiveTrue(@Param("workspaceId") Long workspaceId);

    Optional<WorkspaceCategory> findByWorkspaceIdAndCategoryId(Long workspaceId, Long categoryId);
}
