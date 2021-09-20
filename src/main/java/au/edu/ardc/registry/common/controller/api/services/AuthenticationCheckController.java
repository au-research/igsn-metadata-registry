package au.edu.ardc.registry.common.controller.api.services;

import au.edu.ardc.registry.common.entity.Embargo;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.common.service.*;
import au.edu.ardc.registry.common.util.XMLUtil;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.igsn.exception.IGSNNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@RestController
public class AuthenticationCheckController {

	IdentifierService identifierService;

	ValidationService validationService;

	EmbargoService embargoService;

	VersionService versionService;

	KeycloakService kcService;

	public AuthenticationCheckController(IdentifierService identifierService, ValidationService validationService,
			KeycloakService kcService, EmbargoService embargoService, VersionService versionService) {
		this.identifierService = identifierService;
		this.validationService = validationService;
		this.kcService = kcService;
		this.embargoService = embargoService;
		this.versionService = versionService;
	}

	@GetMapping("/api/services/auth-check")
	public ResponseEntity<?> validateOwnership(@RequestParam(name = "identifier") String identifierValue,
			HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new IGSNNotFoundException(identifierValue);
		}
		Record record = identifier.getRecord();
		boolean result = validationService.validateRecordOwnership(record, user);
		if (!result) {
			throw new ForbiddenOperationException(
					String.format("User %s does not own record %s", user.getId(), record.getId()));
		}

		return ResponseEntity.ok().body(true);
	}

	@GetMapping("/api/services/isPublic")
	public ResponseEntity<?> isPublic(@RequestParam(name = "identifier") String identifierValue
											   ) {
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new IGSNNotFoundException(identifierValue);
		}
		Record record = identifier.getRecord();
		if(!record.isVisible()) {
			throw new ForbiddenOperationException(
					String.format("Record is private %s", record.getId()));
		}
		return ResponseEntity.ok().body(true);
	}

	@GetMapping("/api/services/hasEmbargo")
	public ResponseEntity<?> hasEmbargo(@RequestParam(name = "identifier") String identifierValue) {
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new IGSNNotFoundException(identifierValue);
		}
		Record record = identifier.getRecord();
		Embargo embargo = embargoService.findByRecord(record);
		if (embargo == null){
			return ResponseEntity.ok().body(null);
		}
		return ResponseEntity.ok().body(embargo.getEmbargoEnd().toString());
	}

	@GetMapping("/api/services/getVersionStatus")
	public ResponseEntity<?> getVersionStatus(@RequestParam(name = "identifier") String identifierValue,
											  @RequestParam(name = "schema") String schema) {
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new IGSNNotFoundException(identifierValue);
		}
		String result;
		Record record = identifier.getRecord();
		Version version = versionService.findVersionForRecord(record, schema);
		String contentString =  new String(version.getContent(), StandardCharsets.US_ASCII);
		try {
			NodeList nodeList = XMLUtil.getXPath(contentString, "//logDate");
			Element versionLogDate = (Element) nodeList.item(0);
			result = versionLogDate.getAttribute("eventType");
		} catch (Exception ex) {
			ex.printStackTrace();
			result = null;
		}
		return ResponseEntity.ok().body(result);
	}
}
