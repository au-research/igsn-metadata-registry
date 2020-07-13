package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/resources/versions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Version Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class VersionResourceController {

    @Autowired
    private VersionService versionService;

    @Autowired
    private RecordService recordService;

    @GetMapping("/")
    @Operation(
            summary = "Get all versions",
            description = "Retrieves all versions resources that the current user has access to")
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Version.class)))
    )
    public ResponseEntity<?> index() {
        throw new NotImplementedException();
    }

    @GetMapping(value = "/{id}")
    @Operation(
            summary = "Get a single version by id",
            description = "Retrieve the metadata for a single version by id"
    )
    @ApiResponse(responseCode = "404", description = "Version is not found")
    @ApiResponse(
            responseCode = "200",
            description = "Version is found",
            content = @Content(schema = @Schema(implementation = Version.class))
    )
    public ResponseEntity<?> show(
            @Parameter(
                    required = true,
                    description = "the id of the version (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        Version version = versionService.findById(id);
        if (version == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Version " + id + " is not found");
        }

        return ResponseEntity.ok().body(version);
    }

    @PostMapping("/")
    @Operation(
            summary = "Creates a new version",
            description = "Add a new version to the registry"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Version is created",
            content = @Content(schema = @Schema(implementation = Version.class))
    )
    public ResponseEntity<?> store() {
        throw new NotImplementedException();
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a version by ID",
            description = "Update an existing version"
    )
    @ApiResponse(
            responseCode = "202",
            description = "Version is updated",
            content = @Content(schema = @Schema(implementation = Version.class))
    )
    @ApiResponse(responseCode = "404", description = "Version is not found")
    public ResponseEntity<?> update(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
            @RequestBody Record updatedVersion,
            HttpServletRequest request
    ) {
        // ensure record exists
//        if (!service.exists(id)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
//        }

        throw new NotImplementedException();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a version by ID", description = "Delete a version from the registry")
    @ApiResponse(responseCode = "202", description = "Version is deleted")
    @ApiResponse(responseCode = "404", description = "Version is not found")
    public ResponseEntity<?> destroy(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id
    ) {
//        if (!service.exists(id)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record " + id + " is not found");
//        }
        throw new NotImplementedException();
    }
}
