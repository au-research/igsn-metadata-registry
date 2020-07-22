package au.edu.ardc.igsn.exception;

public class SchemaNotSupportedException extends RuntimeException{
    public SchemaNotSupportedException(String schemaID) {
        super(String.format("Schema %s is not supported", schemaID));
    }
}
