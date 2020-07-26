package au.edu.ardc.igsn.controller.api;

import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.controller.APIController;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.RecordMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.APIExceptionResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private RecordMapper mapper;

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
            @PathVariable String id,
            HttpServletRequest request
    ) {
        User user = kcService.getLoggedInUser(request);
        RecordDTO record = service.findById(id, user);
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
    @ApiResponse(
            responseCode = "403",
            description = "Operation is forbidden",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    public ResponseEntity<RecordDTO> store(
            @RequestBody RecordDTO recordDTO,
            HttpServletRequest request) {
        User user = kcService.getLoggedInUser(request);
        RecordDTO resultDTO = service.create(recordDTO, user);
        return ResponseEntity.created(URI.create("/api/resources/records/" + resultDTO.getId())).body(resultDTO);
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
    public ResponseEntity<RecordDTO> update(
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
            @RequestBody RecordDTO recordDTO,
            HttpServletRequest request
    ) {
        recordDTO.setId(UUID.fromString(id));
        User user = kcService.getLoggedInUser(request);
        RecordDTO resultDTO = service.update(recordDTO, user);
        return ResponseEntity.accepted().body(resultDTO);
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
            @Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
            HttpServletRequest request
    ) {
        User user = kcService.getLoggedInUser(request);
        service.delete(id, user);
        return ResponseEntity.accepted().body(null);
    }

}
