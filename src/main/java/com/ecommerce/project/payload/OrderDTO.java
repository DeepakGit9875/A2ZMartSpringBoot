package com.ecommerce.project.payload;

import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;
    private String email;
    private List<OrderItemDTO> orderItems;
    private LocalDate orderDate;
    private Double totalAmount;
    private String orderStatus;
    private PaymentDTO payment;
    private Long addressId;
}
