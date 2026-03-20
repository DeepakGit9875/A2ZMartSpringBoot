package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("select c from Cart c where c.user.userEmail= ?1")
    Cart findCartByUserEmail(String email);

    @Query("select c from Cart c where c.user.userEmail= ?1 AND c.cartId=?2")
    Cart findCartByUserEmailAndCartId(String emailId, Long cartId);

    @Query("select c from Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product p where p.productId=?1 ")
    List<Cart> findCartsByProductId(Long productId);
}
