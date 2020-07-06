package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.media.UUIDSchema;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources/records")
@Tag(name = "Record Resource API")
@SecurityRequirement(name="basic")
@SecurityRequirement(name="oauth2")
public class RecordResourceController extends APIController {

    @Autowired
    private RecordService service;

    @Autowired
    private KeycloakService kcService;

    @GetMapping("/")
    @Operation(
            summary = "Get all records",
            description = "Retrieves all record resources that the current user has access to")
    public ResponseEntity<?> index(HttpServletRequest request) {
        UUID userID = kcService.getUserUUID(request);

        List<Record> records = service.findOwned(userID);
        return ResponseEntity.ok().body(records);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a single record",
            description = "Retrieve the metadata for a single record by id"
    )
    public ResponseEntity<?> show(
            @Parameter(required = true, schema = @Schema(
                    type = "string",
                    format = "uuid",
                    description = "The UUID of the record"))
            @PathVariable String id
    ) {
        Record record = service.findById(id);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
        }

        return ResponseEntity.ok().body(record);
    }

    @PostMapping("/")
    public ResponseEntity<?> store(
            @RequestParam("allocationID") String allocationIDParam,
            @RequestParam(value = "datacenterID", required = false) String dataCenterIDParam,
            @RequestParam(value = "ownerType", defaultValue = "User") String ownerTypeParam,
            HttpServletRequest request) {

        // todo deal with datacenterIDParam

        // todo validate creatorID has access to allocationID
        UUID creatorID = kcService.getUserUUID(request);

        // todo validate ownerType
        Record.OwnerType ownerType = Record.OwnerType.valueOf(ownerTypeParam);

        Record record = service.create(creatorID, UUID.fromString(allocationIDParam), ownerType);

        URI location = URI.create("/api/resources/records/" + record.getId());

        return ResponseEntity.created(location).body(record);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody Record updatedRecord,
            HttpServletRequest request
    ) {
        // todo validate updatedRecord
        UUID modifierID = kcService.getUserUUID(request);
        Record record = service.findById(id);
        // todo validate record & updatedRecord
        Record updated = service.update(updatedRecord, modifierID);

        return ResponseEntity.ok().body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> destroy(
            @PathVariable String id
    ) {
        // todo validate record exists
        Record record = service.findById(id);

        // todo validate current user and their ownership

        service.delete(record);

        return ResponseEntity.accepted().body(null);
    }

}
