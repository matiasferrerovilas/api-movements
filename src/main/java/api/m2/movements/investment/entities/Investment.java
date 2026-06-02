package api.m2.movements.investment.entities;

import api.m2.movements.entities.commons.Currency;
import api.m2.movements.entities.integrity.User;
import api.m2.movements.entities.integrity.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment", indexes = {
        @Index(name = "idx_investment_workspace", columnList = "workspace_id")
})
@Getter
@Setter
@ToString(exclude = {"currency", "workspace", "owner", "investmentType"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 100)
    private String description;

    @Column(length = 20)
    private String symbol;

    @Column(precision = 10, scale = 4)
    private BigDecimal tna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_type_id", nullable = false)
    private InvestmentType investmentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
