package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.Schema;
import au.edu.ardc.igsn.repository.SchemaRepository;
import au.edu.ardc.igsn.util.XMLValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class SchemaService {

    @Autowired
    SchemaRepository repository;

    public Schema findById(String id) {
        Optional<Schema> opt = repository.findById(id);

        return opt.orElse(null);
    }

    public Iterable<Schema> findAll() {
        return repository.findAll();
    }

    public Schema create(String id, String name) {
        Schema schema = new Schema(id, name);
        schema.setCreated(new Date());
        repository.save(schema);

        return schema;
    }

    public Schema create(Schema schema) {
        schema.setCreated(new Date());
        repository.save(schema);

        return schema;
    }

    public Schema update(Schema schema) {
        repository.save(schema);

        return schema;
    }

    public boolean delete(String id) {
        Schema existed = this.findById(id);
        if (existed == null) {
            return false;
        }

        repository.delete(existed);
        return true;
    }

    /**
     * Validate an XML, given a Schema
     *
     * @param xml          the XML as String
     * @param schema       the Schema, local_path will be used
     * @return boolean
     */
    public boolean validate(String xml, Schema schema) {
        return XMLValidator.validateXMLStringWithXSDPath(xml, schema.getLocal_path());
    }

}
