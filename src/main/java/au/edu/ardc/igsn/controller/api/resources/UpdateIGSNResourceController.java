package au.edu.ardc.igsn.controller.api.resources;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.exception.APIExceptionResponse;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping(value = "/api/resources/igsn", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Version Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class UpdateIGSNResourceController {
	
    @Autowired
    private KeycloakService kcService;
	
    @PostMapping("/update")
    @Operation(
            summary = "Creates new IGSN record(s)",
            description = "Add new IGSN record(s) to the registry"
    )
    @ApiResponse(
            responseCode = "202",
            description = "IGSN Record(s) accepted",
            content = @Content(schema = @Schema(implementation = Record.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "Operation is forbidden",
            content = @Content(schema = @Schema(implementation = APIExceptionResponse.class))
    )
    public ResponseEntity<String> mint(HttpServletRequest request) {
        User user = kcService.getLoggedInUser(request);
        String payload = "";
		try {
			payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
			//TODO test for "Updatability" return with result, if 'updatable" then store the payload and start the mint pipeline 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// for now just send them back what we've received
        return ResponseEntity.status(HttpStatus.SC_ACCEPTED).body(payload);
    }

}
