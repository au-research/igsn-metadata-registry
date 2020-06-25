package au.edu.ardc.igsn.service;

import java.util.List;
import java.util.Optional;

import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.edu.ardc.igsn.exception.RecordNotFoundException;

@Service
public class RecordService{
	
	@Autowired
	private RecordRepository repository;


	/**
	 * Returns all Records
	 *
	 * @return List<Record>
	 */
	public List<Record> findAll() {
		return repository.findAll();
	}
	
	public Optional<Record> findById(String id) {
		return Optional.ofNullable(repository.findById(id)
				.orElseThrow(() -> new RecordNotFoundException(id)));
	}

}
