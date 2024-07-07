package initialise.properties;

import java.util.HashMap;

public class CustomSession {
    HashMap<String,Object> properties;

    //Setter and Getter for the request 
    public Object get(String key){
        return properties.get(key);
    }

    public HashMap<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, Object> properties) {
        this.properties = properties;
    }

    //Function for the session
    public void save(String key,Object value){
        properties.put(key, value);
    }

    public void delete(String key){
        properties.remove(key);
    }

    public void update(String key,Object value){
        properties.put(key, value);
    }

  
}
