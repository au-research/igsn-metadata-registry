package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.model.Schema;

public class MetadataProviderFactory {

    public static Object create(Schema schema, Metadata metadata) {
        try {
            String fqdn = schema.getProviders().get(metadata);
            return Class.forName(fqdn).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // todo throw special exception for this
            e.printStackTrace();
            return null;
        }
    }

}
