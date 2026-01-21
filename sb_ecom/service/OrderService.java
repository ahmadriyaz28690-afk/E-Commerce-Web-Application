package sb_ecom.service;

import jakarta.transaction.Transactional;
import sb_ecom.payload.OrderDTO;

public interface OrderService {
    @Transactional
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);
}
