package sb_ecom.service;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sb_ecom.exceptions.APIException;
import sb_ecom.exceptions.ResourceNotFoundException;
import sb_ecom.model.Cart;
import sb_ecom.model.CartItem;
import sb_ecom.model.Product;
import sb_ecom.payload.CartDTO;
import sb_ecom.payload.ProductDTO;
import sb_ecom.repositories.CartItemRepository;
import sb_ecom.repositories.CartRepository;
import sb_ecom.repositories.ProductRepository;
import sb_ecom.util.AuthUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    AuthUtil authUtil;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        //Find Existing Cart or Create One
        Cart cart = createCart();

        //Retrieve Product Details
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        //Perform Validations
        CartItem cartItem = cartItemRepository.findCartItemBYProductIdAndCartId(
                cart.getCartId(),
                productId
        );
        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + "already exists in the cart");
        }
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available ");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName() + "less than or equal to the quantity" + product.getQuantity() + ".");
        }

        //Create Cart Item
        CartItem newCartItem = new CartItem();

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        //Save Cart Item
        cartItemRepository.save(newCartItem);

        cart.getCartItems().add(newCartItem);

        product.setQuantity(product.getQuantity() + quantity);

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));

        cartRepository.save(cart);

        //Return Update Cart
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });

        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()){
            throw new APIException("No cart exist");
        }

        List<CartDTO> cartDTOS = carts.stream()
                .map(cart -> {
                    CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
                    List<ProductDTO> products = cart.getCartItems().stream()
                            .map(p->modelMapper.map(p.getProduct(),ProductDTO.class))
                            .collect(Collectors.toList());
                          cartDTO.setProducts(products);
                          return cartDTO;
                }).collect(Collectors.toList());
        return cartDTOS;
    }


    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCardId(emailId, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cart.getCartItems().forEach(c-> c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p ->modelMapper.map(p.getProduct(), ProductDTO.class))
                        .collect(Collectors.toList());
                cartDTO.setProducts(products);

        return cartDTO;
    }
    @Transactional // used for multiple work in one Method like ProductCart Quantity Add and Delete
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available ");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName() + "less than or equal to the quantity" + product.getQuantity() + ".");
        }

        CartItem cartItem = cartItemRepository.findCartItemBYProductIdAndCartId(cartId, productId);
        if (cartItem == null){
            throw new APIException("Product" + product.getProductName() + "not available in the cart!!!");
        }

        // Calculate new quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        // Validate to Prevent negative Quantities
        if (newQuantity <0) {
            throw new APIException("The resulting quantity cannot be negative!!!");
        }
        if(newQuantity == 0){
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }
        CartItem updatedItem = cartItemRepository.save(cartItem);
        if (updatedItem.getQuantity() ==0){
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item-> {
                ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
                prd.setQuantity(item.getQuantity());
                return prd;
                });

        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow( ()->new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemBYProductIdAndCartId(cartId, productId);

        if (cartItem == null){
            throw new ResourceNotFoundException("product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);
        return "Product"  + cartItem  .getProduct().  getProductName()  +  "removed from the cart!!!";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemBYProductIdAndCartId(cartId, productId);

        if(cartItem == null){
            throw new APIException("Product" + product. getProductName() + "not available in the cart !!!");
        }

        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice+ (cartItem.getProductPrice()* cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);
    }


    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);

    }
}