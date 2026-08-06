package api.m2.movements.repositories;

import api.m2.movements.entities.WorkspaceCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceCurrencyRepository extends JpaRepository<WorkspaceCurrency, Long> {

    @Query("SELECT wc FROM WorkspaceCurrency wc "
            + "JOIN FETCH wc.currency "
            + "WHERE wc.workspaceId = :workspaceId")
    List<WorkspaceCurrency> findByWorkspaceId(@Param("workspaceId") Long workspaceId);

    Optional<WorkspaceCurrency> findByWorkspaceIdAndCurrencyId(Long workspaceId, Long currencyId);
}
