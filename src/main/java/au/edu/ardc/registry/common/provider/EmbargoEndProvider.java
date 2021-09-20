package au.edu.ardc.registry.common.provider;

import java.util.Date;

public interface EmbargoEndProvider {

	Date get(String content);

}
