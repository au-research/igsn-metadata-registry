package au.edu.ardc.registry.common.provider;

import java.util.Date;

public interface EmbargoProvider {

    Date get(String content);
}
