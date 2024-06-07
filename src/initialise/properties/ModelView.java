package initialise.properties;

import java.util.HashMap;

public class ModelView {
    String url;                         //This is the url of the page 
    HashMap<String,Object> properties;  //Attribut that  contains all the object that will be send to the page

    public ModelView(){
        properties = new HashMap<>();
    }
    //Method that will be used 
    public void addObjet(String key,Object obj) {           //Method that used to add values to the controler
        this.properties.put(key,obj);    
    }

    //Getter and setter 
    public void setUrl(String url) {
        this.url = url;	
    }
    public String getUrl() {
        return this.url;
    }
    public void setProperties(HashMap<String,Object> properties) {
        this.properties = properties;
    }
    public HashMap<String,Object> getProperties() {
        return this.properties;
    }
}