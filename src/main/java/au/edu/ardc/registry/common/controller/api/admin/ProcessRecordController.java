package au.edu.ardc.registry.common.controller.api.admin;

import au.edu.ardc.registry.common.entity.Record;
import au.edu.ardc.registry.common.service.RecordProcessingService;
import au.edu.ardc.registry.common.service.RecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin Operations")
@RequestMapping("/api/admin/process-records")
public class ProcessRecordController {

	@Autowired
	RecordProcessingService recordProcessingService;

	@Autowired
	RecordService recordService;

	@GetMapping("")
	public ResponseEntity<?> handle(@RequestParam(required = false, name = "method") String methodParam,
			@RequestParam(required = false) String id) {

		if (id != null) {
			Record record = recordService.findById(id);
			if (record != null) {
				recordProcessingService.queueRecord(record);
				return ResponseEntity.ok(String.format("Queued record %s", record.getId()));
			}
			else {
				return ResponseEntity.notFound().build();
			}
		}

		// id is null, queue all records
		recordProcessingService.queueAllRecords();

		return ResponseEntity.ok("All records are queued");
	}

}
