package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.entity.Schema;
import au.edu.ardc.igsn.service.SchemaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.net.URI;


@Tag(name = "Schema Resource Controller")
@RestController
@RequestMapping(value = "/api/resources/schemas")
public class SchemaResourceController extends APIController {

    Logger logger = LoggerFactory.getLogger(SchemaResourceController.class);

    @Autowired
    private SchemaService schemaService;

    // TODO Pagination /api/resources/schemas/
    @GetMapping(value = "/")
    public Iterable<Schema> index() {
        logger.info("showing things");
        return schemaService.findAll();
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Schema> show(@PathVariable("id") String id) {
        Schema schema = schemaService.findById(id);
        if (schema == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema " + id + " Not Found");
        }

        return ResponseEntity.ok().body(schema);
    }

    @PostMapping(value = "/")
    public ResponseEntity<Schema> store(@Valid @RequestBody Schema newSchema) {
        Schema schema = schemaService.create(newSchema);

        URI location = URI.create("/api/resources/schemas/" + schema.getId());

        return ResponseEntity.created(location).body(schema);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<Schema> update(@PathVariable("id") String id, @RequestBody Schema updatedSchema) {
        Schema schema = schemaService.update(updatedSchema);

        return ResponseEntity.accepted().body(schema);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") String id) {
        boolean result = schemaService.delete(id);
        if (!result) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.accepted().build();
    }


}
