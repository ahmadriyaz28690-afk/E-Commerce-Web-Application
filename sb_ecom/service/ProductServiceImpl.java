package sb_ecom.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sb_ecom.exceptions.APIException;
import sb_ecom.exceptions.ResourceNotFoundException;
import sb_ecom.model.Cart;
import sb_ecom.model.Category;
import sb_ecom.model.Product;
import sb_ecom.payload.CartDTO;
import sb_ecom.payload.ProductDTO;
import sb_ecom.payload.ProductResponse;
import sb_ecom.repositories.CartRepository;
import sb_ecom.repositories.CategoryRepository;
import sb_ecom.repositories.ProductRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
     private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        //Check if the product already present or not
        Category category =categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));

        boolean isProductNotPresent = true;

        List<Product> products = category.getProductList();
        for (Product value : products) {
            if (value.getProductName().equals(productDTO.getProductName())) {
                isProductNotPresent = false;
                break;
            }
        }

        if (isProductNotPresent) {
            Product product = modelMapper.map(productDTO, Product.class);
            product.setImage("default.png");
            product.setCategory(category);
            double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct, ProductDTO.class);
        } else {
            throw new APIException("Product already exist!!");
        }
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findAll(pageDetails);

        List<Product> products= pageProducts.getContent();
        List<ProductDTO> productDTOS= products.stream().map(product -> modelMapper.map(product,ProductDTO.class)).toList();

        if (products.isEmpty()){
            throw new APIException("No Products Exist!!");
        }

        ProductResponse productResponse =new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElement(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category =categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByCategoryOrderByPriceAsc(category, pageDetails);

        List<Product> products = pageProducts.getContent();

        if (products.isEmpty()){
            throw new APIException(category.getCategoryName()+" category does not have any products");
        }

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product,ProductDTO.class)).toList();

        ProductResponse productResponse =new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElement(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageDetails);

        List<Product> products= pageProducts.getContent();
        List<ProductDTO> productDTOS= products.stream().map(product -> modelMapper.map(product,ProductDTO.class)).toList();

        if (products.isEmpty()){
            throw new APIException("Products not found with keyword: " + keyword);
        }

        ProductResponse productResponse =new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElement(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        //Get the Existing Product from DB
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

        Product product = modelMapper.map(productDTO, Product.class);

        //Update the product info with one in request body
        productFromDb.setProductName(product.getProductName());
        productFromDb.setDescription(product.getDescription());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setDiscount(product.getDiscount());
        productFromDb.setSpecialPrice(product.getSpecialPrice());

        //Save to database
        Product savedProduct = productRepository.save(productFromDb);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);

        List<CartDTO> cartDTOS = carts.stream().map(cart ->{
           CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
           List<ProductDTO> products = cart.getCartItems().stream()
                   .map(p-> modelMapper.map(p.getProduct(), ProductDTO.class))
                   .collect(Collectors.toList());
           cartDTO.setProducts(products);
           return cartDTO;
        }).collect(Collectors.toList());

        cartDTOS.forEach(cart-> cartService.updateProductInCarts(cart.getCartId(), productId)) ;



        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product", "productId",productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        //Get the Product from DB
        Product productFromDB= productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        // Upload Image to Server
        //Get the name of upload Image
        String fileName = fileService.uploadImage(path, image);

        //Updating the new file name to the product
        productFromDB.setImage(fileName);

        //Save Updated Product
        Product updatedProduct =productRepository.save(productFromDB);

        //return DTO after mapping product to DTO
        return modelMapper.map(productId,ProductDTO.class);
    }

    private String uploadImage(String path, MultipartFile file) throws IOException {
        //File names of current/ original file
        String originalFileName = file.getOriginalFilename();

        //Generate a unique file name
        String randomId = UUID.randomUUID().toString();
        //mat.jpg -->1234  --> 1234.jpg
        String fileName = randomId.concat(originalFileName.substring(originalFileName.lastIndexOf('.')));
        String filePath = path + File.separator + fileName;

        //Check if exist and Create
        File folder = new File(path);
        if(!folder.exists())
            folder.mkdir();

        //Upload to Server
        Files.copy(file.getInputStream(), Paths.get(filePath));

        //Returning file name
        return  fileName;
    }
}
