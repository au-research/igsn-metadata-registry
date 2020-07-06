package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.print.attribute.standard.Media;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/resources/records", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Record Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class RecordResourceController extends APIController {

    @Autowired
    private RecordService service;

    @Autowired
    private KeycloakService kcService;

    @GetMapping("/")
    @Operation(
            summary = "Get all records",
            description = "Retrieves all record resources that the current user has access to")
    @ApiResponse(
            responseCode="200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Record.class)))
    )
    public ResponseEntity<?> index(HttpServletRequest request) {
        //todo pagination
        UUID userID = kcService.getUserUUID(request);

        List<Record> records = service.findOwned(userID);
        return ResponseEntity.ok().body(records);
    }

    @GetMapping(value = "/{id}")
    @Operation(
            summary = "Get a single record by id",
            description = "Retrieve the metadata for a single record by id"
    )
    @ApiResponse(responseCode="404", description="Record is not found")
    @ApiResponse(
            responseCode="200",
            description="Record is found",
            content=@Content(schema= @Schema(implementation = Record.class))
    )
    public ResponseEntity<?> show(
            @Parameter(
                    required = true,
                    description = "the id of the record (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        Record record = service.findById(id);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
        }

        return ResponseEntity.ok().body(record);
    }

    @PostMapping("/")
    @Operation(
            summary = "Creates a new record",
            description = "Add a new record to the registry"
    )
    @ApiResponse(
            responseCode="201",
            description="Record is created",
            content=@Content(schema= @Schema(implementation = Record.class))
    )
    public ResponseEntity<?> store(

            @Parameter(name="Allocation ID", schema=@Schema(implementation = UUID.class))
            @RequestParam(name = "allocationID") String allocationIDParam,

            @Parameter(name="DataCenter ID", schema=@Schema(implementation = UUID.class))
            @RequestParam(name = "datacenterID", required = false) String dataCenterIDParam,

            @Parameter(name="Owner Type", description = "The Type of the Owner of this record",
                    schema= @Schema(implementation = Record.OwnerType.class))
            @RequestParam(name="ownerType", defaultValue = "User") String ownerTypeParam,

            HttpServletRequest request) {

        // todo validate ownerType

        Record.OwnerType ownerType = Record.OwnerType.valueOf(ownerTypeParam);
        UUID dataCenterId = null;
        if (ownerType.equals(Record.OwnerType.DataCenter)) {
            dataCenterId = UUID.fromString(dataCenterIDParam);
        }

        // todo validate creatorID has access to allocationID
        UUID creatorID = kcService.getUserUUID(request);

        Record record = service.create(creatorID, UUID.fromString(allocationIDParam), ownerType, dataCenterId);

        URI location = URI.create("/api/resources/records/" + record.getId());

        return ResponseEntity.created(location).body(record);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a record by ID",
            description = "Update an existing record"
    )
    @ApiResponse(
            responseCode="202",
            description="Record is updated",
            content=@Content(schema= @Schema(implementation = Record.class))
    )
    @ApiResponse(responseCode="404", description="Record is not found")
    public ResponseEntity<?> update(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
            @RequestBody Record updatedRecord,
            HttpServletRequest request
    ) {
        // ensure record exists
        if (!service.exists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
        }

        // todo validate updatedRecord
        UUID modifierID = kcService.getUserUUID(request);
        Record record = service.findById(id);
        // todo validate record & updatedRecord
        Record updated = service.update(updatedRecord, modifierID);

        return ResponseEntity.ok().body(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a record by ID", description = "Delete a record from the registry")
    @ApiResponse(responseCode="202", description="Record is deleted")
    @ApiResponse(responseCode="404", description="Record is not found")
    public ResponseEntity<?> destroy(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id
    ) {
        if (!service.exists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
        }
        Record record = service.findById(id);

        // todo validate current user and their ownership

        service.delete(record);

        return ResponseEntity.accepted().body(null);
    }

}
