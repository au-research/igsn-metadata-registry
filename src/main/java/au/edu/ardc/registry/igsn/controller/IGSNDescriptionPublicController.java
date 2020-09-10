package au.edu.ardc.registry.igsn.controller;

import au.edu.ardc.registry.common.controller.api.PageableOperation;
import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.entity.Version;
import au.edu.ardc.registry.exception.NotFoundException;
import au.edu.ardc.registry.common.service.IdentifierService;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.service.VersionService;
import au.edu.ardc.registry.igsn.exception.IGSNNoValidContentForSchema;
import au.edu.ardc.registry.igsn.exception.IGSNNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/public/igsn-description")
@Tag(name = "IGSN Description Public API")
public class IGSNDescriptionPublicController {

	final IdentifierService identifierService;

	final VersionService versionService;

	public IGSNDescriptionPublicController(IdentifierService identifierService, VersionService versionService) {
		this.identifierService = identifierService;
		this.versionService = versionService;
	}

	@GetMapping("")
	@Operation(summary = "Get an IGSN Descriptive content given the identifier",
			description = "Return the content of the version associate with the record for a given identifier")
	@PageableOperation
	public ResponseEntity<?> index(@RequestParam(name = "identifier") String identifierValue,
			@RequestParam(required = false, defaultValue = SchemaService.ARDCv1) String schema) {
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new IGSNNotFoundException(identifierValue);
		}
		Record record = identifier.getRecord();
		Version version = versionService.findVersionForRecord(record, schema);
		if (version == null) {
			throw new IGSNNoValidContentForSchema(identifierValue, schema);
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(version.getContent());
	}

}
