package api.m2.movements.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Marca que un usuario puntual ya vio (y descartó) el cierre de un mes en un workspace. El cierre
 * de mes se calcula on-demand y es igual para todos los miembros, así que el "visto" tiene que ser
 * por (workspace, usuario, año, mes) — no un flag global del período.
 */
@Entity
@Table(
        name = "monthly_summary_seen",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"workspace_id", "user_id", "year", "month"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummarySeen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @CreationTimestamp
    private LocalDateTime seenAt;
}
