package au.edu.ardc.registry.common.controller.api.resources;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.RequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/api/resources/requests")
@Tag(name = "Request Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class RequestResourceController {

	final KeycloakService kcService;

	final RequestService requestService;

	public RequestResourceController(KeycloakService kcService, RequestService requestService) {
		this.kcService = kcService;
		this.requestService = requestService;
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<Request> show(@PathVariable String id, HttpServletRequest httpRequest) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);
		return ResponseEntity.ok().body(request);
	}

}
