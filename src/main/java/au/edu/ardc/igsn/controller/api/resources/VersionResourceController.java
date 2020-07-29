package au.edu.ardc.igsn.controller.api.resources;

import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.dto.VersionMapper;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.exception.APIExceptionResponse;
import au.edu.ardc.igsn.exception.VersionNotFoundException;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.service.KeycloakService;
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
    private KeycloakService kcService;

    @GetMapping("")
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
        User user = kcService.getLoggedInUser(request);
        VersionDTO resultDTO = service.create(versionDTO, user);

        URI location = URI.create("/api/resources/versions/" + resultDTO.getId());
        return ResponseEntity.created(location).body(resultDTO);
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
