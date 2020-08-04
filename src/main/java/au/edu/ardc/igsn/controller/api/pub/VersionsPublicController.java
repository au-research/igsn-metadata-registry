package au.edu.ardc.igsn.controller.api.pub;

import au.edu.ardc.igsn.controller.api.PageableOperation;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.dto.VersionDTO;
import au.edu.ardc.igsn.repository.specs.SearchCriteria;
import au.edu.ardc.igsn.repository.specs.SearchOperation;
import au.edu.ardc.igsn.repository.specs.VersionSpecification;
import au.edu.ardc.igsn.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/public/versions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Versions Public API")
public class VersionsPublicController {

    @Autowired
    VersionService service;

    @GetMapping("")
    @Operation(
            summary = "Get all publicly available versions",
            description = "Return versions from publicly available records")
    @PageableOperation
    public ResponseEntity<Page<VersionDTO>> index(
            @PageableDefault @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) String schema
    ) {
        VersionSpecification specs = new VersionSpecification();
        specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
        specs.add(new SearchCriteria("current", true, SearchOperation.EQUAL));

        if (schema != null) {
            specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
        }

        Page<VersionDTO> result = service.search(specs, pageable);
        return ResponseEntity.ok().body(result);
    }

    // todo @GetMapping("/{id}")
    // todo @GetMapping("/{id}/content")
}
