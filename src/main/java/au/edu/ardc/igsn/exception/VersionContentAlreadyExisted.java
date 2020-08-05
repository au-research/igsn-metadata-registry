package au.edu.ardc.igsn.exception;

public class VersionContentAlreadyExisted extends RuntimeException{
    public VersionContentAlreadyExisted(String schema, String hash) {
        super(String.format("The version's content already existed for schema: %s with hash: %s", schema, hash));
    }
}