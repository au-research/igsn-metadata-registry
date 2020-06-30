package au.edu.ardc.igsn;

public class Schema {

    private final String id;
    private String name;

    public Schema(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }
}
