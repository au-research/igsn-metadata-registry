package au.edu.ardc.igsn.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class Allocation implements Serializable {
    private final UUID id;
    private String name;
    private List<Scope> scopes;
    private String type;
    private String status;

    public Allocation(UUID id) {
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

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
