package api.m2.movements.repositories;

import api.m2.movements.entities.WorkspaceMember;
import api.m2.movements.projections.MembershipSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<WorkspaceMember, Long> {

    @Query("""
        SELECT m
        FROM Workspace a
        JOIN a.members m
        WHERE a.id = :workspaceId
          AND m.user.id = :userId
    """)
    Optional<WorkspaceMember> findMember(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );

    @Query("""
    SELECT
        m.workspace.id as workspaceId,
        m.id as membershipId,
        m.workspace.name as workspaceName,
        m.role as role
    FROM WorkspaceMember m
    WHERE m.user.id = :userId
      AND m.workspace.isActive = true
""")
    List<MembershipSummaryProjection> findAllByUserId(Long userId);

    @Query("SELECT COUNT(m) FROM WorkspaceMember m WHERE m.workspace.id = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
