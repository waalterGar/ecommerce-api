package com.waalterGar.projects.ecommerce.testsupport.builders;

import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderBuilder {
    private String externalId = "ord-" + UUID.randomUUID();
    private OrderStatus status = OrderStatus.CREATED;
    private Currency currency = Currency.EUR;
    private Customer customer = new CustomerBuilder().build();
    private final List<OrderItem> items = new ArrayList<>();

    public OrderBuilder withExternalId(String id) { this.externalId = id; return this; }
    public OrderBuilder withStatus(OrderStatus s) { this.status = s; return this; }
    public OrderBuilder withCurrency(Currency c) { this.currency = c; return this; }
    public OrderBuilder withCustomer(Customer c) { this.customer = c; return this; }
    public OrderBuilder addItem(OrderItem i) { this.items.add(i); return this; }

    public Order build() {
        Order o = new Order();
        o.setExternalId(externalId);
        o.setStatus(status);
        o.setCurrency(currency);
        o.setCustomer(customer); // ensures no NPEs
        o.setItems(List.copyOf(items));
        BigDecimal total = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        o.setTotalAmount(total);
        return o;
    }
}
