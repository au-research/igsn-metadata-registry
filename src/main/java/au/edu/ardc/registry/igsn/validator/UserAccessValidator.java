package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.Scope;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.ValidationService;
import au.edu.ardc.registry.common.util.Helpers;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class UserAccessValidator {

    @Autowired
    ValidationService vService;

    /**
     * @param Identifier
     * @param user
     * @return
     */
    public boolean canCreateIdentifier(String Identifier, User user){
        //TODO get allocation from identifier and check if user has create access
        List<Allocation> allocations = user.getAllocations();
        for(Allocation allocation : allocations){
           Map<String, List<String>> attributes = allocation.getAttributes();
           List<String> prefixes = attributes.get("allocation");
        }

        //Allocation a = new Allocation();
        return true;
    }

    public boolean hasAccess(Record record, User user) throws Exception {
        return vService.validateRecordOwnership(record, user);
    }

    public boolean canCreate(Allocation a, User user) throws Exception {
        return vService.validateAllocationScope(a, user, Scope.CREATE);
    }

    public boolean canUpdate(Allocation a, User user) throws Exception {
        return vService.validateAllocationScope(a, user, Scope.UPDATE);
    }
}
