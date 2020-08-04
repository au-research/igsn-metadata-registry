package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.model.Allocation;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ValidationService {

    /**
     * Validates the ownership of a record
     * User owns the record if the record ownerType is User and their ID matches the ownerID
     * User owns the record if the record OwnerType is DataCenter, and they have access to that DataCenter
     *
     * @param record The full record
     * @param user The User with populated DataCenters
     * @return true if the user owns the record
     */
    public boolean validateRecordOwnership(Record record, User user) {

        // elevated permission the user has ImportScope
        Allocation allocation = new Allocation(record.getAllocationID());
        if (validateAllocationScope(allocation, user, Scope.IMPORT)) {
            return true;
        }

        // OwnerType=User
        if (record.getOwnerType().equals(Record.OwnerType.User) && record.getOwnerID().equals(user.getId())) {
            return true;
        }

        // OwnerType=DataCenter
        return record.getOwnerType().equals(Record.OwnerType.DataCenter)
                && user.belongsToDataCenter(record.getDataCenterID())
                && record.getOwnerID().equals(record.getDataCenterID());
    }

    /**
     * Validates the Scope that the user have access to on an Allocation basis
     *
     * @param allocation The whole Allocation, although only used getId
     * @param user The user with populated Allocation
     * @param scope The Scope that we're checking
     * @return true if the user has access to that scope for the allocation
     */
    public boolean validateAllocationScope(Allocation allocation, User user, Scope scope) {
        UUID allocationID = allocation.getId();
        if (!user.hasAllocation(allocationID)) {
            return false;
        }
        Allocation userAllocation = user.getAllocationById(allocationID);

        return userAllocation.getScopes().contains(scope);
    }

}
