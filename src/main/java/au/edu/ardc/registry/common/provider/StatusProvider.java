package au.edu.ardc.registry.common.provider;

import au.edu.ardc.registry.common.entity.Record;

public interface StatusProvider {
    String get(Record record);
}
