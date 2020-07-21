package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.IdentifierService;
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

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/resources/identifiers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Identifier Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IdentifierResourceController {

    @Autowired
    private IdentifierService service;

    @Autowired
    private RecordService recordService;

    @GetMapping("/")
    @Operation(
            summary = "Get all identifiers",
            description = "Retrieves all identifier resources that the current user has access to")
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Identifier.class)))
    )

    public ResponseEntity<?> index() {
        // todo obtain user from the kcService and find owned from said user
        // todo pagination
        List<Identifier> identifiers = service.findOwned();

        return ResponseEntity.ok(identifiers);
    }

    @GetMapping(value = "/{id}")
    @Operation(
            summary = "Get a single identifier by id",
            description = "Retrieve the metadata for a single identifier by id"
    )
    @ApiResponse(responseCode = "404", description = "Identifier is not found")
    @ApiResponse(
            responseCode = "200",
            description = "Identifier is found",
            content = @Content(schema = @Schema(implementation = Identifier.class))
    )
    public ResponseEntity<?> show(
            @Parameter(
                    required = true,
                    description = "the id of the identifier (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        Identifier identifier = service.findById(id);
        if (identifier == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identifier " + id + " is not found");
        }

        return ResponseEntity.ok().body(identifier);
    }

    @PostMapping("/")
    @Operation(
            summary = "Creates a new identifier",
            description = "Add a new identifier to the registry"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Identifier is created",
            content = @Content(schema = @Schema(implementation = Identifier.class))
    )
    public ResponseEntity<?> store(
            @RequestParam String recordID) {
        Identifier identifier = new Identifier();

        // todo validate record
        Record record = recordService.findById(recordID);
        identifier.setRecord(record);

        // todo if the user has the scope igsn:import, allow direct repository access
        identifier.setCreatedAt(new Date());

        // todo creator
        Identifier createdIdentifier = service.create(identifier);

        URI location = URI.create("/api/resources/identifiers/" + createdIdentifier.getId());

        return ResponseEntity.created(location).body(createdIdentifier);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an identifier by ID", description = "Delete an identifier from the registry")
    @ApiResponse(responseCode = "202", description = "Identifier is deleted")
    @ApiResponse(responseCode = "404", description = "Identifier is not found")
    public ResponseEntity<?> destroy(
            @PathVariable String id
    ) {
        // todo DELETE a resource should always delete it
        if (!service.exists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identifier " + id + " is not found");
        }

        Identifier identifier = service.findById(id);

        service.delete(id);

        return ResponseEntity.accepted().body(identifier);
    }

}
