package au.edu.ardc.igsn.model;

import org.keycloak.representations.idm.authorization.Permission;

import java.util.List;
import java.util.UUID;

public class User {
    private UUID id;
    private String username;
    private String email;
    private String name;
    private List<String> roles;
    private List<Permission> allocations;

    private List<DataCenter> dataCenters;
    private List<Allocation> permissions;

    public User() {

    }

    public User(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public UUID getId() {
        return id;
    }

    public List<Permission> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<Permission> allocations) {
        this.allocations = allocations;
    }

    /**
     * Check whether a User has access to an allocation Permission
     *
     * @param rsID the resource ID
     * @return true if the user has access to the resource
     */
    public boolean hasPermission(String rsID) {
        if (this.allocations == null) {
            return false;
        }
        for (Permission permission : this.allocations) {
            if (permission.getResourceId().equals(rsID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a User has access to the allocation Permission and the provided scope
     *
     * @param rsID  string uuid of the resource
     * @param scope string representation of the scope
     * @return true if the user has access to the resource and the scope
     */
    public boolean hasPermission(String rsID, Scope scope) {
        if (this.allocations == null) {
            return false;
        }
        for (Permission permission : this.allocations) {
            if (permission.getResourceId().equals(rsID) && permission.getScopes().contains(scope.getValue())) {
                return true;
            }
        }
        return false;
    }

    public List<DataCenter> getDataCenters() {
        return dataCenters;
    }

    public void setDataCenters(List<DataCenter> dataCenters) {
        this.dataCenters = dataCenters;
    }

    public List<Allocation> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Allocation> permissions) {
        this.permissions = permissions;
    }

    public boolean belongsToDataCenter(UUID dataCenterID) {
        for (DataCenter dc : dataCenters) {
            if (dc.getId().equals(dataCenterID)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAllocation(UUID allocationID) {
        for (Allocation allocation : permissions) {
            if (allocation.getId().equals(allocationID)) {
                return true;
            }
        }

        return false;
    }

    public Allocation getAllocationById(UUID allocationID) {
        for (Allocation allocation : permissions) {
            if (allocation.getId().equals(allocationID)) {
                return allocation;
            }
        }

        return null;
    }
}
