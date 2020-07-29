package au.edu.ardc.igsn.controller.api.resources;

import au.edu.ardc.igsn.dto.IdentifierDTO;
import au.edu.ardc.igsn.entity.Identifier;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.APIExceptionResponse;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.service.IdentifierService;
import au.edu.ardc.igsn.service.KeycloakService;
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
@RequestMapping(value = "/api/resources/identifiers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Identifier Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class IdentifierResourceController {

    @Autowired
    private IdentifierService service;

    @Autowired
    private KeycloakService kcService;

    @GetMapping("")
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
            @RequestBody IdentifierDTO dto,
            HttpServletRequest request) {
        User user = kcService.getLoggedInUser(request);
        IdentifierDTO resultDTO = service.create(dto, user);
        URI location = URI.create("/api/resources/identifiers/" + resultDTO.getId());
        return ResponseEntity.created(location).body(resultDTO);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an identifier by ID",
            description = "Update an existing identifier"
    )
    @ApiResponse(
            responseCode = "202",
            description = "Identifier is updated",
            content = @Content(schema = @Schema(implementation = Identifier.class))
    )
    @ApiResponse(responseCode = "404", description = "Identifier is not found", content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody Identifier updatedIdentifier,
            HttpServletRequest request
    ) {
        // ensure identifier exists
        if (!service.exists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identifier " + id + " is not found");
        }

        Record record = updatedIdentifier.getRecord();

        // validate record ownership to allocation
        User user = kcService.getLoggedInUser(request);
        UUID allocationID = record.getAllocationID();
        if (!user.hasPermission(allocationID.toString())) {
            throw new ForbiddenOperationException(String.format("User does not have access to the record allocation %s", allocationID.toString()));
        }

        if (!user.hasPermission(allocationID.toString(), Scope.UPDATE)) {
            throw new ForbiddenOperationException(String.format("User does not have access to update for the record allocation %s", allocationID.toString()));
        }

        Identifier updated = service.update(updatedIdentifier);

        return ResponseEntity.ok().body(updated);
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
