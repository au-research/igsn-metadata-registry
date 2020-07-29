package au.edu.ardc.igsn.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Information", description = "Display information about the Registry")
public class InfoController {

    @Autowired
    BuildProperties buildProperties;

    @GetMapping(value = {"/api/info"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Describes the current Registry",
            description = "Display information about the API of the Registry"
    )
    @ApiResponse(responseCode = "200")
    public ResponseEntity<?> index() {
        Map<String, String> build = new HashMap<>();
        build.put("version", buildProperties.getVersion());
        build.put("time", buildProperties.getTime().toString());
        return ResponseEntity.ok(build);
    }
}
