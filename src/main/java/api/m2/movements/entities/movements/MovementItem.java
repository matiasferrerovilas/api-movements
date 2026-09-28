package api.m2.movements.entities.movements;

import api.m2.movements.enums.MovementItemUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Línea de desglose de un {@link Movement} (ej: "1kg papa", "2 tomates"). Referencia a su
 * movimiento por {@code movementId} plano en vez de un {@code @ManyToOne}, igual que
 * {@code Movement.workspaceId} — no hace falta cargar el Movement para leer/escribir sus items.
 */
@Entity
@Table(name = "movement_items", indexes = {
        @Index(name = "idx_movement_item_movement", columnList = "movement_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovementItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "movement_id", nullable = false)
    private Long movementId;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementItemUnit unit;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
