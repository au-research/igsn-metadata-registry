package au.edu.ardc.registry.common.dto;

import au.edu.ardc.registry.common.model.Scope;

import java.util.List;
import java.util.UUID;

public class AllocationDTO {
    private UUID id;

    private String name;

    private List<Scope> scopes;

    private String type;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
