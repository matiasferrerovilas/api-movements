package api.m2.movements.repositories;

import api.m2.movements.entities.Workspace;
import api.m2.movements.projections.WorkspaceSummaryProjection;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    @Query("""
    select distinct a
    from Workspace a
    join fetch a.members
    left join fetch a.owner
    where exists (
        select 1
        from WorkspaceMember m
        where m.workspace = a
          and m.user.id = :userId
    )
    and a.isActive = true
""")
    List<Workspace> findAllWorkspacesByMemberIdWithAllMembers(Long userId);

    @Query("""
    select a from Workspace a
    where a.name = :name
      and a.owner.id = :ownerId
      and a.isActive = true
""")
    Optional<Workspace> findWorkspaceByNameAndOwnerId(@NotNull @Param("name") String name, @Param("ownerId") Long ownerId);

    @Query("""
        select
            a.id as accountId,
            a.name as accountName,
            o.id as ownerId,
            o.email as ownerEmail,
            count(distinct m.id) as membersCount
        from Workspace a
        join a.members m
        join a.owner o
        where a.isActive = true
          and exists (
              select 1
              from WorkspaceMember am
              where am.workspace = a
                and am.user.id = :userId
          )
        group by a.id, a.name, o.id, o.email
""")
    List<WorkspaceSummaryProjection> findWorkspaceSummariesByMemberUserId(@Param("userId") Long userId);
}
