package initialise.properties;
import java.util.HashMap;
import java.util.Map;

public class AttributeException extends Exception {
    private String path;
    private Map<String, String> values;
    private Map<String, String> errors;

    public void setPath(String path) {
        this.path = path;
    }
    public void setValues(Map<String, String> values) {
        this.values = values;
    }
    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
    //Construtor 
    public AttributeException(){
        this.values = new HashMap<>();
        this.values = new HashMap<>();
    }
    public AttributeException(String path, Map<String, String> values, Map<String, String> errors) {
        this.path = path;
        this.values = values;
        this.errors = errors;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        return "Validation errors occurred at path: " + path + ". Errors: " + errors.toString();
    }
}
