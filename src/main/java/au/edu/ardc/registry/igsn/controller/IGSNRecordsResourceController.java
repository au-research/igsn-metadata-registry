package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.controller.api.PageableOperation;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.model.Allocation;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.specs.RecordSpecification;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.igsn.dto.IGSNRecordDTO;
import au.edu.ardc.registry.igsn.dto.mapper.IGSNRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/igsn-records")
@ConditionalOnProperty(name = "app.igsn.enabled")
@Tag(name = "IGSN Records Resource API")
public class IGSNRecordsResourceController {

	private final KeycloakService kcService;

	private final RecordService recordService;

	private final IGSNRecordMapper igsnRecordMapper;

	public IGSNRecordsResourceController(KeycloakService kcService, RecordService recordService,
			IGSNRecordMapper igsnRecordMapper) {
		this.kcService = kcService;
		this.recordService = recordService;
		this.igsnRecordMapper = igsnRecordMapper;
	}

	@GetMapping("")
	@Operation(summary = "Get all IGSN Records",
			description = "Retrieves all IGSN records that the current user has access to")
	@PageableOperation
	public ResponseEntity<Page<IGSNRecordDTO>> index(HttpServletRequest request,
			@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String title) {
		RecordSpecification specs = new RecordSpecification();
		User user = kcService.getLoggedInUser(request);

		// build ownerIDs list
		List<UUID> ownerIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		ownerIDs.add(user.getId());

		// building a search specification, by default ownerID in the provided list
		specs.add(new SearchCriteria("ownerID", ownerIDs, SearchOperation.IN));
		specs.add(new SearchCriteria("type", "IGSN", SearchOperation.EQUAL));

		if (title != null) {
			specs.add(new SearchCriteria("title", title, SearchOperation.MATCH));
		}

		// perform the search
		Page<Record> result = recordService.search(specs, pageable);

		return ResponseEntity.ok().body(result.map(igsnRecordMapper.getConverter()));
	}

}
