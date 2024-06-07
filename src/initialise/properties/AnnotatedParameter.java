package initialise.properties;

public class AnnotatedParameter {
    private final String name;
    private final Class<?> type;

    public AnnotatedParameter(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }
}