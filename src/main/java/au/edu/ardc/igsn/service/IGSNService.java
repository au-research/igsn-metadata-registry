package au.edu.ardc.igsn.service;

import au.edu.ardc.igsn.config.IGSNProperties;
import au.edu.ardc.igsn.entity.IGSNServiceRequest;
import au.edu.ardc.igsn.model.User;
import au.edu.ardc.igsn.repository.IGSNServiceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class IGSNService {

    Logger logger = LoggerFactory.getLogger(IGSNService.class);

    @Autowired
    IGSNProperties IGSNProperties;

    @Autowired
    private IGSNServiceRequestRepository repository;

    public IGSNServiceRequest findById(String id) {
        Optional<IGSNServiceRequest> opt = repository.findById(UUID.fromString(id));
        return opt.orElse(null);
    }

    public IGSNServiceRequest createRequest(User user) {
        // create IGSNServiceRequest
        logger.debug("Creating IGSNServiceRequest for user: {}", user);
        IGSNServiceRequest request = new IGSNServiceRequest();
        request.setCreatedAt(new Date());
        request.setCreatedBy(user.getId());
        request = repository.save(request);
        logger.debug("Created IGSNServiceRequest: id: {}", request.getId());

        // create request directory
        UUID id = request.getId();
        try {
            logger.debug("Creating data path");
            String separator = System.getProperty("file.separator");
            Path path = Paths.get(IGSNProperties.getDataPath() + separator + id.toString());
            logger.debug("Creating data path: {}", path.toAbsolutePath());
            Files.createDirectories(path);
            logger.debug("Created data path: {}", path.toAbsolutePath());
            request.setDataPath(path.toAbsolutePath().toString());
            // todo store the data path to the IGSNServiceRequest
        } catch (IOException e) {
            logger.error("Failed creating data path {}", e.getMessage());
        }

        request = repository.save(request);
        return request;
    }

}
