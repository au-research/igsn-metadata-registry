package au.edu.ardc.igsn.exception;

public class VersionNotFoundException extends RuntimeException{
    public VersionNotFoundException(String uuid) {
        super(String.format("Version with uuid:%s is not found within the registry", uuid));
    }
}
