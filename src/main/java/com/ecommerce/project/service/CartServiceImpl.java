package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    CartRepository cartRepository;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart=createCart();

        Product product= productRepository.findById(productId).
                orElseThrow(() -> new ResourceNotFoundException("product", "productId", productId));

        CartItem cartItem=cartItemRepository.findItemByProductIdAndCartId(
            cart.getCartId(),
                productId
        );

        if(cartItem!=null){
            throw new APIException("Product "+product.getProductName()+" already exists in the cart");
        }

        if(product.getQuantity()==0){
            throw new APIException(product.getProductName()+" is not available");
        }

        if(product.getQuantity()<quantity){
            throw new APIException("Please, make an order of the product "+product.getProductName()+
                    " less than or equal to quantity "+product.getQuantity()+".");
        }

         CartItem newCartItem= new CartItem();

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        product.setQuantity(product.getQuantity()- quantity);

        cart.setTotalPrice(cart.getTotalPrice()+(product.getSpecialPrice()*quantity));

        cartRepository.save(cart);

        CartDTO cartDTO=modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems=cart.getCartItems();
        Stream<ProductDTO> productStream= cartItems.stream().map(item ->
        {
            ProductDTO map=modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });

        cartDTO.setProducts(productStream.toList());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts=cartRepository.findAll();

        if(carts.size()==0){
            throw new APIException("No cart exists as of now");
        }

        List<CartDTO> cartDTOs= carts.stream()
                .map(cart -> {
                    CartDTO cartDTO=modelMapper.map(cart, CartDTO.class);
                    List<ProductDTO> products= cart.getCartItems().stream()
                            .map(p ->
                                modelMapper.map(p.getProduct(), ProductDTO.class)
                            ).collect(Collectors.toList());
                    cartDTO.setProducts(products);
                    return cartDTO;
                }).collect(Collectors.toList());
        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {

        Cart cart=cartRepository.findCartByUserEmailAndCartId(emailId,cartId);
        if(cart==null){
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }
        CartDTO cartDTO=modelMapper.map(cart, CartDTO.class);

        cart.getCartItems().forEach(c-> c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> productDTO=cart.getCartItems().stream()
                .map(p ->
                    modelMapper.map(p.getProduct(),ProductDTO.class)
                ).toList();
        cartDTO.setProducts(productDTO);
        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String email= authUtil.loggedInEmail();
        Cart userCart=cartRepository.findCartByUserEmail(email);
        Long cartId=userCart.getCartId();
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product= productRepository.findById(productId).
                orElseThrow(() -> new ResourceNotFoundException("product", "productId", productId));

        if(product.getQuantity()==0){
            throw new APIException(product.getProductName()+" is not available");
        }

        if(product.getQuantity()<quantity){
            throw new APIException("Please, make an order of the product "+product.getProductName()+
                    " less than or equal to quantity "+product.getQuantity()+".");
        }

        CartItem cartItem=cartItemRepository.findItemByProductIdAndCartId(cartId,productId);
        if(cartItem==null){
            throw new APIException("Product "+product.getProductName()+" not available in the cart!!");
        }

        // Calculate new quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        // Validation to prevent negative quantities
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative.");
        }

        if (newQuantity == 0){
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());

            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }
        CartItem updatedItem=cartItemRepository.save(cartItem);

        if(updatedItem.getQuantity()==0){
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }

        CartDTO cartDTO=modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems=cart.getCartItems();

        Stream<ProductDTO> productDTOStream=cartItems.stream().map(
                item -> {
                    ProductDTO prd=modelMapper.map(item.getProduct(),ProductDTO.class);
                    prd.setQuantity(item.getQuantity());
                    return prd;
                });
        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));

        CartItem cartItem=cartItemRepository.findItemByProductIdAndCartId(cartId,productId);
        if(cartItem==null){
            throw new ResourceNotFoundException("Product","productId",productId);
        }
        cart.setTotalPrice(cart.getTotalPrice()-
                (cartItem.getProductPrice()*cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        return "Product "+cartItem.getProduct().getProductName()+" removed from Cart!!";
    }

    @Override
    public void updateProductsInCarts(Long cartId, Long productId) {
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product= productRepository.findById(productId).
                orElseThrow(() -> new ResourceNotFoundException("product", "productId", productId));

        CartItem cartItem=cartItemRepository.findItemByProductIdAndCartId(cartId,productId);

        if(cartItem==null){
            throw new APIException("Product "+product.getProductName()+" not available in the cart!!");
        }

        double cartPrice=cart.getTotalPrice()-
                (cartItem.getProductPrice()*cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice +
                (cartItem.getProductPrice()*cartItem.getQuantity()));

        cartItemRepository.save(cartItem);

    }

    public Cart createCart(){
        Cart userCart=cartRepository.findCartByUserEmail(authUtil.loggedInEmail());
        if(userCart!=null){
            return userCart;
        }

        Cart cart=new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart= cartRepository.save(cart);
        return newCart;
    }
}
