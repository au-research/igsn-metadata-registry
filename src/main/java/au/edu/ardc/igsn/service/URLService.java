package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.entity.URL;
import au.edu.ardc.igsn.repository.URLRepository;
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
