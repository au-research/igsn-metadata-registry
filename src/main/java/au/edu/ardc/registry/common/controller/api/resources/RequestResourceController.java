package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.mapper.IdentifierMapper;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.specs.IdentifierSpecification;
import au.edu.ardc.registry.common.repository.specs.RecordSpecification;
import au.edu.ardc.registry.common.repository.specs.SearchCriteria;
import au.edu.ardc.registry.common.repository.specs.SearchOperation;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.util.Helpers;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping(value = "/api/resources/requests")
@Tag(name = "Request Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class RequestResourceController {

	final KeycloakService kcService;

	final RequestService requestService;

	final RecordService recordService;

	final RecordMapper recordMapper;

	final IdentifierService identifierService;

	final IdentifierMapper identifierMapper;

	public RequestResourceController(KeycloakService kcService, RequestService requestService,
			RecordService recordService, RecordMapper recordMapper, IdentifierService identifierService,
			IdentifierMapper identifierMapper) {
		this.kcService = kcService;
		this.requestService = requestService;
		this.recordService = recordService;
		this.recordMapper = recordMapper;
		this.identifierService = identifierService;
		this.identifierMapper = identifierMapper;
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<Request> show(@PathVariable String id, HttpServletRequest httpRequest) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);
		return ResponseEntity.ok().body(request);
	}

	@GetMapping(value = "/{id}/logs")
	public ResponseEntity<String> showLogs(@PathVariable String id, HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		String dataPath = request.getAttributes().get("dataPath");
		String logsPath = dataPath + "/logs";
		File logFile = new File(dataPath + "/logs");
		if (!logFile.exists()) {
			throw new RuntimeException(String.format("Logs Path: %s doesn't exist", logsPath));
		}
		String logContent = Helpers.readFile(logsPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

	@GetMapping(value = "/{id}/records")
	public ResponseEntity<Page<RecordDTO>> showRecords(@PathVariable String id, HttpServletRequest httpRequest,
			Pageable pageable) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		RecordSpecification specs = new RecordSpecification();
		specs.add(new SearchCriteria("requestID", request.getId(), SearchOperation.EQUAL));
		Page<Record> result = recordService.search(specs, pageable);
		Page<RecordDTO> resultDTO = result.map(recordMapper.getConverter());

		return ResponseEntity.ok().body(resultDTO);
	}

	@GetMapping(value = "/{id}/identifiers")
	public ResponseEntity<Page<IdentifierDTO>> showIdentifiers(@PathVariable String id, HttpServletRequest httpRequest,
			Pageable pageable) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		IdentifierSpecification specs = new IdentifierSpecification();
		specs.add(new SearchCriteria("requestID", request.getId(), SearchOperation.RECORD_EQUAL));
		Page<IdentifierDTO> result = identifierService.search(specs, pageable);

		return ResponseEntity.ok(result);
	}

}
