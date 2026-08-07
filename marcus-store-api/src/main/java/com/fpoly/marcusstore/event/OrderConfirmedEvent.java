package com.fpoly.marcusstore.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderConfirmedEvent extends ApplicationEvent {
    private final Integer orderId;

    public OrderConfirmedEvent(Object source, Integer orderId) {
        super(source);
        this.orderId = orderId;
    }
}
