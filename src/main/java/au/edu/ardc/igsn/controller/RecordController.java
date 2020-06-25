package au.edu.ardc.igsn.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import au.edu.ardc.igsn.service.RecordService;
import au.edu.ardc.igsn.entity.Record;

@RestController
public class RecordController {
	
	Logger logger = LoggerFactory.getLogger(RecordController.class);
	
	@Autowired
	private RecordService recordService;
	
	
	@GetMapping("/")
    public String index() {
        return "Wilkommen zu IGSN 2.0";
    }
	
    @GetMapping("/record/all")
    public List<Record> getRecords() {
//		System.out.print("getting All Records");
		logger.info("Getting All Records");
    	List<Record> records = recordService.findAll();
    	return records;
    }
    
	@GetMapping("/record/{id}")
	public Optional<Record> getRecordById(@PathVariable String id) {
		return recordService.findById(id);
	}

}
