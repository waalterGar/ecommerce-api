package com.waalterGar.projects.ecommerce.entity;


import com.waalterGar.projects.ecommerce.utils.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orderitem_order"))
    private Order order;

    @Column(nullable = false, length = 50)
    private String productSku;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    /** Compute once, in service, before persisting */
    public void computeLineTotal() {
        if (unitPrice == null || quantity == null) {
            throw new IllegalStateException("unitPrice and quantity must be set before computing lineTotal");
        }
        lineTotal = unitPrice
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
