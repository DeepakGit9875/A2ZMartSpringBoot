package com.ecommerce.project.service;

import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.repositories.CartRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

public interface OrderService {

    @Transactional
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);
}
