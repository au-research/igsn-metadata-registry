package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.common.dto.IdentifierDTO;
import au.edu.ardc.registry.common.dto.RecordDTO;
import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.IdentifierMapper;
import au.edu.ardc.registry.common.dto.mapper.RecordMapper;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Identifier;
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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;

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

	final RequestMapper requestMapper;

	public RequestResourceController(KeycloakService kcService, RequestService requestService,
			RecordService recordService, RecordMapper recordMapper, IdentifierService identifierService,
			IdentifierMapper identifierMapper, RequestMapper requestMapper) {
		this.kcService = kcService;
		this.requestService = requestService;
		this.recordService = recordService;
		this.recordMapper = recordMapper;
		this.identifierService = identifierService;
		this.identifierMapper = identifierMapper;
		this.requestMapper = requestMapper;
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<RequestDTO> show(@PathVariable String id, HttpServletRequest httpRequest) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping(value = "/{id}/logs")
	public ResponseEntity<String> showLogs(@PathVariable String id, HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		String logPath = requestService.getLoggerPathFor(request);
		File logFile = new File(logPath);
		if (!logFile.exists()) {
			throw new RuntimeException(String.format("Logs Path: %s doesn't exist", logPath));
		}
		String logContent = Helpers.readFile(logPath);

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
		Page<Identifier> result = identifierService.search(specs, pageable);
		Page<IdentifierDTO> resultDTO = result.map(identifierMapper.getConverter());
		return ResponseEntity.ok(resultDTO);
	}

	@PostMapping(value = "/")
	public ResponseEntity<RequestDTO> store(@RequestBody RequestDTO requestDTO, HttpServletRequest httpServletRequest) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request = requestService.create(requestDTO, user);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		URI location = URI.create("/api/resources/requests/" + dto.getId().toString());
		return ResponseEntity.created(location).body(dto);
	}

	@PutMapping(value = "/{id}")
	public ResponseEntity<RequestDTO> update(@PathVariable String id, @RequestBody RequestDTO requestDTO,
			HttpServletRequest httpServletRequest) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request = requestService.findById(id);
		Request updatedRequest = requestService.update(request, requestDTO, user);

		RequestDTO dto = requestMapper.getConverter().convert(updatedRequest);
		return ResponseEntity.accepted().body(dto);
	}

	@PostMapping(value = "{id}/logs")
	public ResponseEntity<String> appendLog(@PathVariable String id, @RequestBody String message,
			HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		requestService.getLoggerFor(request).info(message);
		requestService.closeLoggerFor(request);

		String logPath = requestService.getLoggerPathFor(request);
		String logContent = Helpers.readFile(logPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

}
