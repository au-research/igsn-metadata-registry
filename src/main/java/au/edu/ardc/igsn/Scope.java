package au.edu.ardc.igsn;

public enum Scope {
    UPDATE("igsn:update"),
    CREATE("igsn:create"),
    IMPORT("igsn:import");

    private final String value;

    Scope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
