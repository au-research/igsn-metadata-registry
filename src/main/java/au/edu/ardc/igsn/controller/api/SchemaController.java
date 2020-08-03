package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.model.Schema;
import au.edu.ardc.igsn.service.SchemaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Schema API")
@RestController
@RequestMapping("/api/resources/schemas")
public class SchemaController {

    @Autowired
    SchemaService service;

    @GetMapping("")
    public ResponseEntity<?> getSupportedSchemas() {
        List<Schema> schemas = service.getSupportedSchemas();

        return ResponseEntity.ok().body(schemas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schema> getSchemaByID(@PathVariable String id) {
        if (!service.supportsSchema(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema " + id + " not found or not supported");
        }

        Schema schema = service.getSchemaByID(id);
        return ResponseEntity.ok().body(schema);
    }

    @PostMapping(value = "/{id}/validate")
    public ResponseEntity<?> validateSchema(@PathVariable String id, @RequestBody String payload) {

        // TODO handle malformed XML here?
        if (!service.supportsSchema(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema " + id + " not found or not supported");
        }

        Schema schema = service.getSchemaByID(id);

        // TODO handle schema validation exception
        if (service.validate(schema, payload)) {
            return ResponseEntity.ok().body(null);
        } else {
            return ResponseEntity.badRequest().body("Validation Failed");
        }
    }

}
