package au.edu.ardc.igsn.model;

import java.util.UUID;

public class DataCenter {
    private final UUID id;
    private String name;

    public DataCenter(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
