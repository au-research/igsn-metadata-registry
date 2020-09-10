package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.VersionDTO;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.exception.APIExceptionResponse;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.specs.RecordSpecification;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.common.repository.specs.VersionSpecification;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/records")
@Tag(name = "Record Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class RecordResourceController {

	@Autowired
	private RecordService recordService;

	@Autowired
	private KeycloakService kcService;

	@Autowired
	private IdentifierService identifierService;

	@Autowired
	private VersionService versionService;

	@Autowired
	ApplicationProperties applicationProperties;

	@GetMapping("")
	@Operation(summary = "Get all records",
			description = "Retrieves all record resources that the current user has access to")
	@ApiResponse(responseCode = "200",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecordDTO.class))))
	public ResponseEntity<Page<?>> index(HttpServletRequest request,
			@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String title) {
		// obtain a list of ownerIDs include the current user ownerID
		User user = kcService.getLoggedInUser(request);
		List<UUID> ownerIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		ownerIDs.add(user.getId());

		// building a search specification, by default ownerID in the provided list
		RecordSpecification specs = new RecordSpecification();
		specs.add(new SearchCriteria("ownerID", ownerIDs, SearchOperation.IN));
		if (title != null) {
			specs.add(new SearchCriteria("title", title, SearchOperation.MATCH));
		}

		// perform the search
		Page<RecordDTO> result = recordService.search(specs, pageable);

		return ResponseEntity.ok().body(result);
	}

	@GetMapping(value = "/{id}")
	@Operation(summary = "Get a single record by id", description = "Retrieve the metadata for a single record by id")
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "Record is found",
			content = @Content(schema = @Schema(implementation = RecordDTO.class)))
	public ResponseEntity<RecordDTO> show(
			@Parameter(required = true, description = "the id of the record (uuid)",
					schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		RecordDTO record = recordService.findById(id, user);
		return ResponseEntity.ok().body(record);
	}

	@PostMapping("/")
	@Operation(summary = "Creates a new record", description = "Add a new record to the registry")
	@ApiResponse(responseCode = "201", description = "Record is created",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "403", description = "Operation is forbidden",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<RecordDTO> store(@RequestBody RecordDTO recordDTO, HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		RecordDTO resultDTO = recordService.create(recordDTO, user);
		return ResponseEntity.created(URI.create("/api/resources/records/" + resultDTO.getId())).body(resultDTO);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a record by ID", description = "Update an existing record")
	@ApiResponse(responseCode = "202", description = "Record is updated",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<RecordDTO> update(
			@Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			@RequestBody RecordDTO recordDTO, HttpServletRequest request) {
		recordDTO.setId(UUID.fromString(id));
		User user = kcService.getLoggedInUser(request);
		RecordDTO resultDTO = recordService.update(recordDTO, user);
		return ResponseEntity.accepted().body(resultDTO);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a record by ID", description = "Delete a record from the registry")
	@ApiResponse(responseCode = "202", description = "Record is deleted")
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<?> destroy(@Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		recordService.delete(id, user);
		return ResponseEntity.accepted().body(null);
	}

	@GetMapping(value = "/{id}/versions")
	public ResponseEntity<?> showVersions(Pageable pageable, @PathVariable String id,
			@RequestParam(required = false) String schema) {
		Record record = recordService.findById(id);
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("record", record, SearchOperation.EQUAL));
		if (schema != null) {
			specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		}
		Page<VersionDTO> result = versionService.search(specs, pageable);
		return ResponseEntity.ok().body(result);
	}

}
