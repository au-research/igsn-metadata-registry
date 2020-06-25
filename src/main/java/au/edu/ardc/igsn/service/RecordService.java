package au.edu.ardc.igsn.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.entity.Record;

@Service
public class RecordService{
	
	@Autowired
	private RecordRepository repository;
	

	public List<Record> findAll() {
		return (List<Record>) repository.findAll();
	}
	
	public Optional<Record> findById(String id) {
		return Optional.ofNullable(repository.findById(id)
				.orElseThrow(() -> new RecordNotFoundException(id)));
	}

}
