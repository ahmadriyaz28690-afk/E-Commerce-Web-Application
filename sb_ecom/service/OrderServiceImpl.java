package sb_ecom.service;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sb_ecom.exceptions.APIException;
import sb_ecom.exceptions.ResourceNotFoundException;
import sb_ecom.model.*;
import sb_ecom.payload.OrderDTO;
import sb_ecom.payload.OrderItemDTO;
import sb_ecom.repositories.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    CartRepository cartRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartService cartService;

    @Autowired
    ModelMapper modelMapper;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        //Getting User Cart
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null){
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address", "addressId", addressId));

        //Create a new order with payment info
        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted !");
        order.setAddress(address);

        Payment payment= new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        //Get Items from the Cart info the order items
        List<CartItem> cartItems = cart.getCartItems();
        if(cartItems.isEmpty()){
            throw new APIException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for(CartItem cartItem : cartItems){
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);

        }
       orderItems = orderItemRepository.saveAll(orderItems);

        //Update Product Stock
        cart.getCartItems().forEach(item-> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);

            //Clear the Cart
            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());

        });

        //Send back the Order Summary
        OrderDTO orderDTO= modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(item-> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)
        ));
        orderDTO.setAddressId(addressId);

        return orderDTO;
    }
}
