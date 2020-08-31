package au.edu.ardc.registry.common.controller.api.pub;

import au.edu.ardc.registry.common.controller.api.PageableOperation;
import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.repository.specs.IdentifierSpecification;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.common.service.IdentifierService;
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
@RequestMapping(value = "/api/public/identifiers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Identifier Public API")
public class IdentifiersPublicController {

    @Autowired
    IdentifierService identifierService;

    @GetMapping("")
    @Operation(
            summary = "Get all publicly available records",
            description = "Retrieves all publicly available records")
    @PageableOperation
    public ResponseEntity<Page<IdentifierDTO>> index(
            @PageableDefault @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String value) {
        IdentifierSpecification specs = new IdentifierSpecification();
        specs.add(new SearchCriteria("status", Identifier.Status.ACCESSIBLE, SearchOperation.EQUAL));

        // todo sanitize type
        if (type != null) {
            specs.add(new SearchCriteria("type", Identifier.Type.valueOf(type), SearchOperation.EQUAL));
        }

        if (value != null) {
            specs.add(new SearchCriteria("value", value, SearchOperation.EQUAL));
        }

        Page<IdentifierDTO> result = identifierService.search(specs, pageable);
        return ResponseEntity.ok().body(result);
    }
}
