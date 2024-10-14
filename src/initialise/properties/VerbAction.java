package initialise.properties;

public class VerbAction {
    String nameMethod;
    String verb;

    //constructor
    public VerbAction(){
        this.verb = "None";
    }

    //Getter and Setter 
    public void setNameMethod(String nameMethod){
        this.nameMethod = nameMethod;
    }

    public String getNameMethod(){
        return this.nameMethod;
    }

    public void setVerb(String verb){
        this.verb = verb;
    }

    public String getVerb(){
        return this.verb;
    }
}
