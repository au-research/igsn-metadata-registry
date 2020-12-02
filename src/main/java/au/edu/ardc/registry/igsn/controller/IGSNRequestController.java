package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.igsn.service.IGSNService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.core.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/resources/igsn-requests",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@ConditionalOnProperty(name = "app.igsn.enabled")
@Tag(name = "IGSN Request Resource", description = "API endpoints to interact with IGSN related Requests")
public class IGSNRequestController {

	final KeycloakService kcService;

	final RequestService requestService;

	final IGSNService igsnService;

	final RequestMapper requestMapper;

	public IGSNRequestController(KeycloakService kcService, RequestService requestService, IGSNService igsnService,
			RequestMapper requestMapper) {
		this.kcService = kcService;
		this.requestService = requestService;
		this.igsnService = igsnService;
		this.requestMapper = requestMapper;
	}

	@PutMapping(value = "/{id}")
	@Operation(summary = "Restart a completed or failed IGSN Request", description = "Rerun an IGSN single/bulk mint or update request",
			parameters = {@Parameter(name = "status", description = "the required action",
							schema = @Schema(description = "status", type = "string", allowableValues = { "RESTART" }, defaultValue = "RESTART"))})
	public ResponseEntity<RequestDTO> update(@PathVariable String id,
			@RequestParam(name = "status") String status,
			@RequestBody Optional<RequestDTO> requestDTO, HttpServletRequest httpServletRequest) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request = requestService.findById(id);
		RequestDTO dto = null;
		Logger requestLog = requestService.getLoggerFor(request);
		Date date = new Date();
		// archive log at log.millieseconds
		// empty the log file
		// set request status to RESTARTED
		// re-run the request
		if (status.equals("RESTART") && request.getType().startsWith("igsn")) {
			try {
				requestLog.info("Restarted Request at: " + date);
				String logPath = requestService.getLoggerPathFor(request);
				File logFile = new File(logPath);
				File acrhived = new File(logFile.getParentFile().getPath() + File.separator + "logs." + date.getTime());
				InputStream is = new FileInputStream(logFile);
				OutputStream os = new FileOutputStream(acrhived);
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				PrintWriter pw = new PrintWriter(logFile);
				pw.close();
				requestLog.info("Restarting Request at: " + date);
			}
			catch (Exception e) {

				requestLog.info("Couldn't archive existing logs");
			}
			request.setStatus(Request.Status.RESTARTED);
			request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, 0);
			request.setAttribute(Attribute.NUM_OF_RECORDS_CREATED, 0);
			request.setAttribute(Attribute.NUM_OF_RECORDS_UPDATED, 0);
			request.setAttribute(Attribute.NUM_OF_IGSN_REGISTERED, 0);
			request.setAttribute(Attribute.NUM_OF_ERROR, 0);
			request.setAttribute(Attribute.NUM_OF_RECORDS_FORBIDDEN, 0);
			request.setAttribute(Attribute.NUM_OF_FAILED_REGISTRATION, 0);
			igsnService.processMintOrUpdate(request);
			dto = requestMapper.getConverter().convert(request);
		}
		else if (requestDTO.isPresent()) {
			Request updatedRequest = requestService.update(request, requestDTO.get(), user);
			dto = requestMapper.getConverter().convert(updatedRequest);
		}

		return ResponseEntity.accepted().body(dto);
	}

}
