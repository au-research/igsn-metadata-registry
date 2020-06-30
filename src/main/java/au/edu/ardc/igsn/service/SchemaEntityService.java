package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.SchemaEntity;
import au.edu.ardc.igsn.repository.SchemaRepository;
import au.edu.ardc.igsn.util.XMLValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class SchemaEntityService {

    @Autowired
    SchemaRepository repository;

    public SchemaEntity findById(String id) {
        Optional<SchemaEntity> opt = repository.findById(id);

        return opt.orElse(null);
    }

    public Iterable<SchemaEntity> findAll() {
        return repository.findAll();
    }

    public SchemaEntity create(String id, String name) {
        SchemaEntity schema = new SchemaEntity(id, name);
        schema.setCreated(new Date());
        repository.save(schema);

        return schema;
    }

    public SchemaEntity create(SchemaEntity schema) {
        schema.setCreated(new Date());
        repository.save(schema);

        return schema;
    }

    public SchemaEntity update(SchemaEntity schema) {
        repository.save(schema);

        return schema;
    }

    public boolean delete(String id) {
        SchemaEntity existed = this.findById(id);
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
    public boolean validate(String xml, SchemaEntity schema) {
        return XMLValidator.validateXMLStringWithXSDPath(xml, schema.getLocal_path());
    }

}
