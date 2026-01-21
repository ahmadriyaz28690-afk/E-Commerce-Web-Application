package sb_ecom.exceptions;


public class ResourceNotFoundException extends RuntimeException{
    String resourceName;
    String field;
    String fieldName;
    Long fieldId;

    public ResourceNotFoundException(String cart, String email, String emailId){

    }

    public ResourceNotFoundException(String message, Throwable cause, String resourceName, String field, String fieldName) {
        super(String.format("%s not found with %s: %s", resourceName,field,fieldName));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldName = fieldName;
    }

    public ResourceNotFoundException(String message, Throwable cause, String resourceName, String field, Long fieldId) {
        super(String.format("%s not found with %s: %d", resourceName,field,fieldId));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldId = fieldId;
    }

    public ResourceNotFoundException(String category, String categoryId, Long categoryId1) {
    }
}
