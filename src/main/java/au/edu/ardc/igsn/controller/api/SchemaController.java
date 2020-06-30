package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.Schema;
import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.LinkedHashMap;
import java.util.List;

@Controller
@RequestMapping("/api/schemas")
public class SchemaController extends APIController {

    @Autowired
    SchemaService service;

    @GetMapping("/")
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

}
