package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.common.controller.api.PageableOperation;
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
import au.edu.ardc.registry.common.repository.specs.*;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RecordService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.common.util.Helpers;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping(value = "/api/resources/requests",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
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

	@GetMapping(value = "/")
	@PageableOperation
	public ResponseEntity<Page<RequestDTO>> index(HttpServletRequest httpServletRequest, @SortDefault.SortDefaults({
			@SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) }) Pageable pageable) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		RequestSpecification specs = new RequestSpecification();
		specs.add(new SearchCriteria("createdBy", user.getId(), SearchOperation.EQUAL));
		Page<Request> result = requestService.search(specs, pageable);
		Page<RequestDTO> dtos = result.map(requestMapper.getConverter());
		return ResponseEntity.ok(dtos);
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
	@PageableOperation
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
	@PageableOperation
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

	@PostMapping(value = "{id}/logs")
	public ResponseEntity<String> appendLog(@PathVariable String id, @RequestBody String message,
			HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);
		message = Helpers.getLine(message, 0);
		requestService.getLoggerFor(request).info(message);
		requestService.closeLoggerFor(request);

		String logPath = requestService.getLoggerPathFor(request);
		String logContent = Helpers.readFile(logPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

}
