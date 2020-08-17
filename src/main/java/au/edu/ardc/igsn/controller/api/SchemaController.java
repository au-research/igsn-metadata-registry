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
    public ResponseEntity<?> index() {
        List<Schema> schemas = service.getSchemas();

        return ResponseEntity.ok().body(schemas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schema> show(@PathVariable String id) {
        if (!service.supportsSchema(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema " + id + " not found or not supported");
        }

        Schema schema = service.getSchemaByID(id);
        return ResponseEntity.ok().body(schema);
    }
}
