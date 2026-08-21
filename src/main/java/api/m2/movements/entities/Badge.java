package api.m2.movements.entities;

import api.m2.movements.entities.commons.Category;
import api.m2.movements.enums.BadgeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A workspace-level achievement earned for a closed period (e.g. "stayed under the Comida budget
 * in August") — not tied to a specific user, since budgets themselves are shared across the
 * workspace, not owned by whoever happened to log the movements.
 */
@Entity
@Table(name = "badges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"workspace_id", "category_id", "year", "month", "type"})
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeType type;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @CreationTimestamp
    @Column(name = "earned_at")
    private LocalDateTime earnedAt;
}
