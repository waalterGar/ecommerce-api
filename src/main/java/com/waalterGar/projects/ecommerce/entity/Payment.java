package com.waalterGar.projects.ecommerce.entity;

import com.waalterGar.projects.ecommerce.utils.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_tx_ref", columnList = "transaction_reference")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payments_order_txref",
                        columnNames = {"order_id", "transaction_reference"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "transaction_reference", length = 200)
    private String transactionReference;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

}
