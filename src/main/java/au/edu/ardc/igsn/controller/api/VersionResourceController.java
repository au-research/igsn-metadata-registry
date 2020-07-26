package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.model.Scope;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.dto.VersionMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.*;
import au.edu.ardc.igsn.service.KeycloakService;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.SchemaService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/resources/versions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Version Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class VersionResourceController {

    @Autowired
    VersionMapper versionMapper;
    @Autowired
    private VersionService service;
    @Autowired
    private RecordService recordService;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private KeycloakService kcService;

    @GetMapping("/")
    @Operation(
            summary = "Get all versions",
            description = "Retrieves all versions resources that the current user has access to")
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Version.class)))
    )
    public ResponseEntity<?> index() {
        // todo obtain user from the kcService and find owned from said user
        // todo pagination
        List<Version> versions = service.findOwned();

        return ResponseEntity.ok(versions);
    }

    @GetMapping(value = "/{id}")
    @Operation(
            summary = "Get a single version by id",
            description = "Retrieve the metadata for a single version by id"
    )
    @ApiResponse(responseCode = "404", description = "Version is not found", content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
    @ApiResponse(
            responseCode = "200",
            description = "Version is found",
            content = @Content(schema = @Schema(implementation = Version.class))
    )
    public ResponseEntity<VersionDTO> show(
            @Parameter(
                    required = true,
                    description = "the id of the version (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        Version version = service.findById(id);
        if (version == null) {
            throw new VersionNotFoundException(id);
        }
        VersionDTO versionDTO = versionMapper.convertToDTO(version);
        return ResponseEntity.ok().body(versionDTO);
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
    public ResponseEntity<?> store(
            @Valid @RequestBody VersionDTO versionDTO,
            HttpServletRequest request) {

        // validate the schema
        if (versionDTO.getSchema() == null || !schemaService.supportsSchema(versionDTO.getSchema())) {
            throw new SchemaNotSupportedException(versionDTO.getSchema());
        }

        // validate record
        if (versionDTO.getRecord() == null || !recordService.exists(versionDTO.getRecord())) {
            throw new RecordNotFoundException(versionDTO.getRecord());
        }
        Record record = recordService.findById(versionDTO.getRecord());

        // validate record ownership to allocation
        User user = kcService.getLoggedInUser(request);
        UUID allocationID = record.getAllocationID();
        if (!user.hasPermission(allocationID.toString())) {
            throw new ForbiddenOperationException(String.format("User does not have access to the record allocation %s", allocationID.toString()));
        }

        if (!user.hasPermission(allocationID.toString(), Scope.CREATE)) {
            throw new ForbiddenOperationException(String.format("User does not have access to create for the record allocation %s", allocationID.toString()));
        }

        // todo validate record ownership

        // todo validate versionDTO content

        Version version = versionMapper.convertToEntity(versionDTO);
        version.setCreatedAt(new Date());
        version.setCreatorID(user.getId());

        // allow igsn:import scope to overwrite data
        if (user.hasPermission(allocationID.toString(), Scope.IMPORT)) {
            version.setCreatedAt(versionDTO.getCreatedAt() != null ? versionDTO.getCreatedAt() : version.getCreatedAt());
            version.setCreatorID(versionDTO.getCreatorID() != null ? UUID.fromString(versionDTO.getCreatorID()) : version.getCreatorID());
            version.setStatus(versionDTO.getStatus() != null ? versionDTO.getStatus() : version.getStatus());
        }

        Version createdVersion = service.create(version);

        VersionDTO createdVersionDTO = versionMapper.convertToDTO(createdVersion);
        URI location = URI.create("/api/resources/versions/" + createdVersion.getId());
        return ResponseEntity.created(location).body(createdVersionDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a version by ID", description = "Delete a version from the registry")
    @ApiResponse(responseCode = "202", description = "Version is deleted")
    @ApiResponse(responseCode = "404", description = "Version is not found", content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
    public ResponseEntity<?> destroy(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id
    ) {
        // todo consider PUT /{id} with {state:ENDED} to end a version instead
        // todo DELETE a resource should always delete it
        if (!service.exists(id)) {
            throw new VersionNotFoundException(id);
        }

        Version version = service.findById(id);

        // upon deleting a version, end the version instead
        version = service.end(version);

        return ResponseEntity.accepted().body(version);
    }

    @GetMapping(value = "/{id}/content", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Get a version content", description = "Get a version content by ID")
    @ApiResponse(responseCode = "200", description = "Version content is found and delivered")
    @ApiResponse(responseCode = "404", description = "Version is not found", content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
    public ResponseEntity<?> content(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id
    ) {
        if (!service.exists(id)) {
            throw new VersionNotFoundException(id);
        }
        Version version = service.findById(id);

        String content = new String(version.getContent());

        return ResponseEntity.ok(content);
    }

}
