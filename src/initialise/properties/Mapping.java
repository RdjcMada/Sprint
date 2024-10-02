package initialise.properties;

public class Mapping {
    String nameClass;
    String nameMethod; 
    String verb;

    public Mapping(){
        this.verb = "None";
    }
    public void add(String n1, String n2) {
        this.nameClass = n1;
        this.nameMethod = n2;
    }

    public String getValue() {
        return nameMethod;
    }

    public String getKey(){
        return nameClass;
    }

    public void setVerb(String verb)  {
        this.verb = verb;
    }

    public String getVerb() {
        return verb;
    }
}
