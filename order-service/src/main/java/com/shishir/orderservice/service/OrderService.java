package com.shishir.orderservice.service;

import com.shishir.orderservice.dto.InventoryResponse;
import com.shishir.orderservice.dto.OrderLineItemsDto;
import com.shishir.orderservice.dto.OrderRequest;
import com.shishir.orderservice.dto.OrderResponse;
import com.shishir.orderservice.event.OrderPlacedEvent;
import com.shishir.orderservice.model.Order;
import com.shishir.orderservice.model.OrderLineItems;
import com.shishir.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .build();

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToModel).collect(Collectors.toList());
        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodeList = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).collect(Collectors.toList());

        Span inventoryLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryLookup.start())) {
// call inventory service and create order if inventory is present in stock
            InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodeList).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            if (null != inventoryResponses) {
                boolean isInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
                if (isInStock) {
                    orderRepository.save(order);

                    kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));

                    return "Order Placed Successfully.";
                }
            }

            throw new IllegalArgumentException("Product not in stock, please try again.");
        } finally {
            inventoryLookup.end();
        }
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

    private OrderLineItemsDto mapToDto(OrderLineItems orderLineItems) {
        return OrderLineItemsDto.builder()
                .id(orderLineItems.getId())
                .price(orderLineItems.getPrice())
                .skuCode(orderLineItems.getSkuCode())
                .quantity(orderLineItems.getQuantity())
                .build();
    }

    private OrderResponse mapToResponseDto(Order order) {
        List<OrderLineItemsDto> orderLineItemsDtoList = order.getOrderLineItemsList().stream().map(this::mapToDto).collect(Collectors.toList());
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderLineItemsDtoList(orderLineItemsDtoList)
                .build();
    }

}
