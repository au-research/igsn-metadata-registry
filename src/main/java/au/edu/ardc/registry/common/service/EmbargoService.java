package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.entity.Embargo;
import au.edu.ardc.registry.common.entity.Record;
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
	 * @param date to compare the embargoEnd to
	 * @return The Lists of Embargo to end
	 */
	public List<Embargo> findAllEmbargoToEnd(Date date) {
		return repository.findAllByEmbargoEndLessThanEqual(date);
	}

	/**
	 * Return List of embargo that have reached their embargoEnd date
	 * @param embargoList of embargo to end
	 */
	public void endEmbargoList(List<Embargo> embargoList) {
		for(Embargo endEmbargo: embargoList) {
			Record record = endEmbargo.getRecord();
			record.setVisible(true);
			delete(endEmbargo.getId().toString());
		}
	}

	/**
	 * Permanently delete the embargo
	 * @param id the uuid of the Embargo
	 */
	public void delete(String id) {
		repository.deleteById(id);
	}

}
