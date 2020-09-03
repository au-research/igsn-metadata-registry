package au.edu.ardc.registry.common.controller.api.services;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.KeycloakService;
import au.edu.ardc.registry.common.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/services/auth-check")
public class AuthenticationCheckController {

	IdentifierService identifierService;

	ValidationService validationService;

	KeycloakService kcService;

	public AuthenticationCheckController(IdentifierService identifierService, ValidationService validationService,
			KeycloakService kcService) {
		this.identifierService = identifierService;
		this.validationService = validationService;
		this.kcService = kcService;
	}

	@GetMapping("")
	public ResponseEntity<?> validateOwnership(@RequestParam(name = "identifier") String identifierValue,
			HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new NotFoundException("IGSN with value " + identifierValue + " is not found");
		}
		Record record = identifier.getRecord();
		boolean result = validationService.validateRecordOwnership(record, user);
		if (!result) {
			throw new ForbiddenOperationException(
					String.format("User %s does not own record %s", user.getId(), record.getId()));
		}

		return ResponseEntity.ok().body(true);
	}

}
