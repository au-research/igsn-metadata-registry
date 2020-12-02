package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.dto.RequestDTO;
import au.edu.ardc.registry.common.dto.mapper.RequestMapper;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import au.edu.ardc.registry.exception.APIExceptionResponse;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.service.IGSNService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
	@Operation(summary = "Restart a completed or failed IGSN Request",
			description = "Interact with any existing IGSN request (Only RESTART is supported as of v1.0)",
			parameters = {@Parameter(name = "status", description = "The state which the Request is set to",
							schema = @Schema(description = "status", type = "string", allowableValues = { "RESTART" }, defaultValue = "RESTART"))},
			responses = {
					@ApiResponse(responseCode = "200", description = "Request has restarted",
							content = @Content(schema = @Schema(implementation = RequestDTO.class))),
					@ApiResponse(responseCode = "403", description = "Forbidden Operation Exception either:<br/>" +
							"User is not the owner of the Request<br/>" +
							"The Request is not Completed yet<br/>" +
							"the value of the parameter 'status' is not supported (only RESTART is supported as of v1.0)<br/>" +
							"The request type is not IGSN only mint, bulk-mint, update, bulk-update, reserve and transfer requests are Supported",
							content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))) })
	public ResponseEntity<RequestDTO> update(@PathVariable String id,
			@RequestParam(name = "status") String status, HttpServletRequest httpServletRequest) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request = requestService.findOwnedById(id, user);
		Logger requestLog = requestService.getLoggerFor(request);
		Date date = new Date();
		// archive log at log.millieseconds
		// empty the log file
		// set request status to RESTARTED
		// re-run the request
		if(!request.getStatus().equals(Request.Status.COMPLETED) && !request.getStatus().equals(Request.Status.FAILED)){
			throw new ForbiddenOperationException("Only COMPLETED or FAILED Requests can be restarted");
		}
		else if(!status.equals("RESTART")){
			throw new ForbiddenOperationException("Only RESTART is supported");
		}
		else if(!request.getType().startsWith("igsn")){
			throw new ForbiddenOperationException("Only IGSN requests are supported");
		}
		// all should be ready to proceed
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
		RequestDTO dto = requestMapper.getConverter().convert(request);

		return ResponseEntity.accepted().body(dto);
	}

}
