package au.edu.ardc.registry.igsn.validator;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PayloadValidator {

    @Autowired
    ContentValidator cValidator;

    @Autowired
    UserAccessValidator uaValidator;

    private List<String> identifiers;

    public boolean isvalidPayload(String content) throws Exception {
        boolean isValid = true;
        // Validate the entire content even if it contains multiple resources
        isValid = cValidator.validate(content);
        return isValid;
    }

    public boolean hasUserAccess(String content, User user) throws Exception {

        Schema schema = cValidator.service.getSchemaForContent(content);
        IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
        this.identifiers = provider.getAll(content);
        for(int i = 0 ; i < this.identifiers.size(); i++)
        {
            if(!uaValidator.canCreateIdentifier(this.identifiers.get(i), user)){
                return false;
            }
        }
        return true;
    }
}
