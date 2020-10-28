package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.entity.Embargo;
import au.edu.ardc.registry.common.repository.EmbargoRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class EmbargoService {

	private final EmbargoRepository repository;

	public EmbargoService(EmbargoRepository repository) {
		this.repository = repository;
	}

	/**
	 * Return List of embargo that have reached their embargoEnd date
	 */
	public List<Embargo> findAllEmbargoToEnd() {
		Date now = new Date();
		return repository.findAllByEmbargoEndLessThanEqual(now);
	}

	/**
	 * Permanently delete the embargo
	 * @param id the uuid of the Embargo
	 */
	public void delete(String id) {
		repository.deleteById(id);
	}

}
