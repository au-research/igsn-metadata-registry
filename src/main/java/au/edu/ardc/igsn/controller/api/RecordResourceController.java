package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.User;
import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.APIExceptionResponse;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            responseCode = "200",
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
    @ApiResponse(
            responseCode = "404",
            description = "Record is not found",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Record is found",
            content = @Content(schema = @Schema(implementation = Record.class))
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
            throw new RecordNotFoundException(id);
        }

        return ResponseEntity.ok().body(record);
    }

    @PostMapping("/")
    @Operation(
            summary = "Creates a new record",
            description = "Add a new record to the registry"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Record is created",
            content = @Content(schema = @Schema(implementation = Record.class))
    )
    public ResponseEntity<?> store(

            @RequestBody Record newRecord,
            HttpServletRequest request) {

        User user = kcService.getLoggedInUser(request);

        // validate the user has access to allocationID
        String allocationID = newRecord.getAllocationID().toString();
        if (!user.hasPermission(allocationID)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("you don't have access to %s", allocationID)
            );
        }

        // validate user has access to the igsn:create scope
        if (!user.hasPermission(allocationID, "igsn:create")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("you don't have access to create resource for %s", allocationID)
            );
        }
        // todo validate OwnerType && datacenterID

        // if the user has access to igsn:import scope, directly insert it
        if (user.hasPermission(allocationID, "igsn:import")) {
            Record record = service.create(newRecord);
            return ResponseEntity.created(URI.create("/api/resources/records/" + record.getId())).body(record);
        } else {
            // create it normally
            Record record = service.create(user.getId(), newRecord.getAllocationID());
            return ResponseEntity.created(URI.create("/api/resources/records/" + record.getId())).body(record);
        }
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a record by ID",
            description = "Update an existing record"
    )
    @ApiResponse(
            responseCode = "202",
            description = "Record is updated",
            content = @Content(schema = @Schema(implementation = Record.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Record is not found",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    public ResponseEntity<?> update(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
            @RequestBody Record updatedRecord,
            HttpServletRequest request
    ) {
        // ensure record exists
        if (!service.exists(id)) {
            throw new RecordNotFoundException(id);
        }

        // todo validate updatedRecord
        UUID modifierID = kcService.getUserUUID(request);
        //Record record = service.findById(id);
        // todo validate record & updatedRecord
        Record updated = service.update(updatedRecord, modifierID);

        return ResponseEntity.ok().body(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a record by ID", description = "Delete a record from the registry")
    @ApiResponse(responseCode = "202", description = "Record is deleted")
    @ApiResponse(
            responseCode = "404",
            description = "Record is not found",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    public ResponseEntity<?> destroy(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id
    ) {
        if (!service.exists(id)) {
            throw new RecordNotFoundException(id);
        }
        Record record = service.findById(id);

        // todo validate current user and their ownership

        service.delete(record);

        return ResponseEntity.accepted().body(null);
    }

}
