package au.edu.ardc.registry.common.controller.api.pub;

import au.edu.ardc.registry.common.controller.api.PageableOperation;
import au.edu.ardc.registry.common.dto.VersionDTO;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.exception.APIExceptionResponse;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.common.repository.specs.VersionSpecification;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/public/versions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Versions Public API")
public class VersionsPublicController {

    @Autowired
    VersionService service;

    @Autowired
    RecordService recordService;

    @GetMapping("")
    @Operation(
            summary = "Get all publicly available versions",
            description = "Return versions from publicly available records")
    @PageableOperation
    public ResponseEntity<Page<VersionDTO>> index(
            @PageableDefault @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) String schema,
            @RequestParam(required = false) String record
    ) {
        VersionSpecification specs = new VersionSpecification();
        specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
        specs.add(new SearchCriteria("current", true, SearchOperation.EQUAL));

        if (schema != null) {
            specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
        }

        if (record != null) {
            Record recordEntity = recordService.findById(record);
            specs.add(new SearchCriteria("record", recordEntity, SearchOperation.EQUAL));
        }

        Page<VersionDTO> result = service.search(specs, pageable);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a single public version by id",
            description = "Retrieve the metadata for a single version by id"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Version is not found",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Version is found",
            content = @Content(schema = @Schema(implementation = VersionDTO.class))
    )
    public ResponseEntity<VersionDTO> show(
            @Parameter(
                    required = true,
                    description = "the id of the version (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        VersionDTO dto = service.findPublicById(id);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("/{id}/content")
    @Operation(
            summary = "Display the content of a public version",
            description = "Return the content of the public version"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Version is not found",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Version is found",
            content = @Content(schema = @Schema(implementation = VersionDTO.class))
    )
    public ResponseEntity<?> showContent(
            @Parameter(
                    required = true,
                    description = "the id of the version (uuid)",
                    schema = @Schema(implementation = UUID.class)
            )
            @PathVariable String id
    ) {
        // reuse the logic for finding public version
        VersionDTO dto = service.findPublicById(id);

        Version version = service.findById(dto.getId());
        return ResponseEntity.ok().body(version.getContent());
    }
}
