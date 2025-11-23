package pl.koszela.nowoczesnebud.Exception;

/**
 * Wyjątek rzucany gdy zasób nie zostanie znaleziony
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s nie znaleziono: %s = '%s'", resourceName, fieldName, fieldValue));
    }
}


























