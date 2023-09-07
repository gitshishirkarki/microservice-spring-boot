package com.shishir.orderservice.service;

import com.shishir.orderservice.dto.OrderLineItemsDto;
import com.shishir.orderservice.dto.OrderRequest;
import com.shishir.orderservice.dto.OrderResponse;
import com.shishir.orderservice.model.Order;
import com.shishir.orderservice.model.OrderLineItems;
import com.shishir.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    public void placeOrder(OrderRequest orderRequest){
        Order order  = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .build();

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToModel).collect(Collectors.toList());
        order.setOrderLineItemsList(orderLineItemsList);

        orderRepository.save(order);
    }

    public List<OrderResponse> getOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private OrderLineItems mapToModel(OrderLineItemsDto orderLineItemsDto) {
        return OrderLineItems.builder()
                .price(orderLineItemsDto.getPrice())
                .skuCode(orderLineItemsDto.getSkuCode())
                .quantity(orderLineItemsDto.getQuantity())
                .build();
    }

    private OrderLineItemsDto mapToDto(OrderLineItems orderLineItems){
        return OrderLineItemsDto.builder()
                .id(orderLineItems.getId())
                .price(orderLineItems.getPrice())
                .skuCode(orderLineItems.getSkuCode())
                .quantity(orderLineItems.getQuantity())
                .build();
    }

    private OrderResponse mapToResponseDto(Order order){
        List<OrderLineItemsDto> orderLineItemsDtoList = order.getOrderLineItemsList().stream().map(this::mapToDto).collect(Collectors.toList());
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderLineItemsDtoList(orderLineItemsDtoList)
                .build();
    }

}
