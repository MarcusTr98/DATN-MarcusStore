package com.fpoly.marcusstore.event;

import com.fpoly.marcusstore.entity.shopping.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderConfirmedEvent extends ApplicationEvent {
    private final Order order;

    public OrderConfirmedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order order() {
        return order;
    }
}