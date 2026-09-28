package api.m2.movements.repositories;

import api.m2.movements.entities.movements.MovementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovementItemRepository extends JpaRepository<MovementItem, Long> {
    List<MovementItem> findByMovementIdOrderByIdAsc(Long movementId);

    List<MovementItem> findByMovementIdInOrderByIdAsc(List<Long> movementIds);

    void deleteByMovementId(Long movementId);
}
