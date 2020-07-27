package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.dto.URLDTO;
import au.edu.ardc.igsn.dto.URLMapper;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.URL;
import au.edu.ardc.igsn.exception.ForbiddenOperationException;
import au.edu.ardc.igsn.exception.RecordNotFoundException;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.URLRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class URLService {

    @Autowired
    private URLRepository repository;

    @Autowired
    private URLMapper mapper;

    @Autowired
    ValidationService validationService;

    @Autowired
    RecordService recordService;

    /**
     * Find a url by id
     *
     * @param id the uuid of the URL
     * @return the URL if it exists, null if not
     */
    public URL findById(String id) {
        Optional<URL> opt = repository.findById(UUID.fromString(id));

        return opt.orElse(null);
    }

    /**
     * Tell if a URL exists by id
     *
     * @param id the uuid of the URL
     * @return if the uuid correlate to an existing url
     */
    public boolean exists(String id) {
        return repository.existsById(UUID.fromString(id));
    }

    /**
     * Retrieve all owned URLs
     * Owned URLs are the URLs that which records the user have access to
     *
     * todo accept User UUID as a parameter
     * todo update findOwned at the repository level
     * @return a list of URLs that is owned by the user
     */
    public List<URL> findOwned() {
        return repository.findAll();
    }

    // create
    public URL create(URL newUrl) {
        return repository.save(newUrl);
    }

    public URLDTO create(URLDTO dto, User user) {
        URL url = mapper.convertToEntity(dto);

        // validate record existence
        if (!recordService.exists(dto.getRecord().toString())) {
            throw new RecordNotFoundException(dto.getRecord().toString());
        }

        // validate record ownership
        Record record = recordService.findById(dto.getRecord().toString());
        if (!validationService.validateRecordOwnership(record, user)) {
            throw new ForbiddenOperationException("User does not have access to create URL for this record");
        }

        //defaults
        url.setRecord(record);
        url.setCreatedAt(new Date());
        url.setResolvable(false);

        // todo import

        url = repository.save(url);
        return mapper.convertToDTO(url);
    }

    /**
     * Update a URL
     *
     * @param url to be updated
     * @return The url that has updated
     */

    public URL update(URL url) {
        url.setUpdatedAt(new Date());
        repository.save(url);
        return url;
    }

    /**
     * Permanently delete the url
     *
     * @param id the uuid of the URL
     */
    public void delete(String id) {
        repository.deleteById(id);
    }

}
