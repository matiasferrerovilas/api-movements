package api.m2.movements.investment.entities;

import api.m2.movements.entities.integrity.Workspace;
import api.m2.movements.investment.enums.InvestmentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "investment_type", indexes = {
        @Index(name = "idx_investment_type_workspace", columnList = "workspace_id")
})
@Getter
@Setter
@ToString(exclude = {"workspace"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvestmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "icon_name", nullable = false)
    @Builder.Default
    private String iconName = "QuestionOutlined";

    @Column(name = "icon_color", nullable = false)
    @Builder.Default
    private String iconColor = "#d9d9d9";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
