package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources/records")
public class RecordResourceController extends APIController {

    @Autowired
    RecordService service;

    @GetMapping("/")
    public ResponseEntity<?> index() {
        List<Record> records = service.findAll();

        return ResponseEntity.ok().body(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> show(@PathVariable String id) {
        Record record = service.findById(id);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
        }

        return ResponseEntity.ok().body(record);
    }

    @PostMapping("/")
    public ResponseEntity<?> store() {
        UUID creatorID = UUID.randomUUID();
        UUID allocationID = UUID.randomUUID();

        Record record = service.create(creatorID, allocationID, Record.OwnerType.User);

        URI location = URI.create("/api/resources/records/" + record.getId());

        return ResponseEntity.created(location).body(record);
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Record updatedRecord) {
        throw new NotImplementedException();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> destroy(@PathVariable String id) {
        throw new NotImplementedException();
    }

}
