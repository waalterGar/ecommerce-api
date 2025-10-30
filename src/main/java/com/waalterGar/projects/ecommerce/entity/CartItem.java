package com.waalterGar.projects.ecommerce.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "cart_items",
        indexes = {
                @Index(name = "ix_cart_items_cart_id", columnList = "cart_id"),
                @Index(name = "ix_cart_items_sku", columnList = "sku")
        },
        uniqueConstraints = {
                // A cart should not contain the same SKU twice
                @UniqueConstraint(name = "uk_cart_items_cart_sku", columnNames = {"cart_id", "sku"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_CartItem_order"))
    private Cart cart;

    @Column(name = "sku", nullable = false, length = 50)
    private String productSku;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    @Version
    private Long version;

    public void computeLineTotal() {
        if (unitPrice == null || quantity == null) {
            throw new IllegalStateException("unitPrice and quantity must be set before computing lineTotal");
        }
        this.lineTotal = unitPrice
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
