package initialise.properties;

import java.util.List;

public class Mapping {
    String nameClass;
    List<VerbAction> verbActions ; 

    public void add(String n1, List<VerbAction> n2) {
        this.nameClass = n1;
        this.verbActions = n2;
    }

    public String getKey(){
        return nameClass;
    }

    public void setVerbActions(List<VerbAction> actions){
        this.verbActions = actions;
    }

    public List<VerbAction> getVerbActions(){
        return this.verbActions;
    }
}
