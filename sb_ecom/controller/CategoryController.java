package sb_ecom.controller;


import jakarta.validation.Valid;
import sb_ecom.config.AppConstants;
import sb_ecom.payload.CategoryDTO;
import sb_ecom.payload.CategoryResponse;
import sb_ecom.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/public/categories")
    public ResponseEntity <CategoryResponse> getAllCategories(
            @RequestParam(name = "pageNumber" , defaultValue = AppConstants.PAGE_NUMBER,required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE,required = false)Integer pageSize ,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORIES_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR,required = false ) String sortOrder){
        CategoryResponse categoryResponse = categoryService.getAllCategories(pageNumber ,pageSize, sortBy,sortOrder);
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);

    }

    @PostMapping("/public/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO){
        CategoryDTO savedCategory = categoryService.createCategory(categoryDTO);
        return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
    }


    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory (@PathVariable Long categoryId){
        try{
        CategoryDTO deleteCategory= categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(deleteCategory, HttpStatus.OK);
    } catch (ResponseStatusException e){
        return new ResponseEntity<>(null,e.getStatusCode());
        }
    }

    @PutMapping("/public/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@RequestBody CategoryDTO categoryDTO,
                                                      @PathVariable Long categoryId){

        try {
            CategoryDTO savedCategoryDTO = categoryService.updateCategory(categoryDTO, categoryId);
            return new ResponseEntity<>(savedCategoryDTO, HttpStatus.OK);

        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(null, e.getStatusCode());
        }
    }


}

