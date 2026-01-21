package sb_ecom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sb_ecom.payload.OrderDTO;
import sb_ecom.payload.OrderRequestDTO;
import sb_ecom.service.OrderService;
import sb_ecom.util.AuthUtil;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    AuthUtil authUtil;

    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod, @RequestBody OrderRequestDTO orderRequestDTO) {
        String emailId = authUtil.loggedInEmail();
       OrderDTO order = orderService.placeOrder(
                emailId,
                orderRequestDTO.getAddressId(),
                paymentMethod,
                orderRequestDTO.getPgName(),
                orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(),
                orderRequestDTO.getPgResponseMessage()


        );
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
}