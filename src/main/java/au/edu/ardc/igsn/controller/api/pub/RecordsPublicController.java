package au.edu.ardc.igsn.controller.api.pub;

import au.edu.ardc.igsn.controller.api.PageableOperation;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.APIExceptionResponse;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/public/records", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Record Public API")
public class RecordsPublicController {

    @Autowired
    RecordService service;

    @Autowired
    VersionService versionService;

    @GetMapping("")
    @Operation(
            summary = "Get all publicly available records",
            description = "Retrieves all publicly available records")
    @PageableOperation
    public ResponseEntity<Page<RecordDTO>> index(@PageableDefault @Parameter(hidden = true) Pageable pageable) {
        Page<RecordDTO> result = service.findPublic(pageable);
        return ResponseEntity.ok().body(result);
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
            content = @Content(schema = @Schema(implementation = RecordDTO.class))
    )
    public ResponseEntity<RecordDTO> show(
            @Parameter(
                    required = true,
                    description = "the id of the record (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        RecordDTO dto = service.findPublicById(id);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping(value = "/{id}/versions")
    @Operation(
            summary = "Get all versions for a record",
            description = "Retrieve the versions for a single record by id"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Record is not found",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Versions are found",
            content = @Content(schema = @Schema(implementation = Page.class))
    )
    public ResponseEntity<?> showVersions(
            @Parameter(
                    required = true,
                    description = "the id of the record (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id,
            Pageable pageable
    ) {
        // try to reuse the business logic of finding public record
        RecordDTO dto = service.findPublicById(id);

        Record record = service.getMapper().convertToEntity(dto);
        Page<VersionDTO> result = versionService.findAllVersionsForRecord(record, pageable);
        return ResponseEntity.ok().body(result);
    }
}