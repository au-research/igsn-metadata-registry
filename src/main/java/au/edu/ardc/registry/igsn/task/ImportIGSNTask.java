package au.edu.ardc.registry.igsn.task;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.event.RecordUpdatedEvent;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.igsn.event.IGSNUpdatedEvent;
import au.edu.ardc.registry.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;

public class ImportIGSNTask implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(ImportIGSNTask.class);

    private File file;

    private Request request;

    ApplicationEventPublisher applicationEventPublisher;

    ImportService importService;

    private String identifierValue;

    public ImportIGSNTask(String identifierValue, File file, Request request, ImportService importService, ApplicationEventPublisher applicationEventPublisher) {
        this.identifierValue = identifierValue;
        this.file = file;
        this.request = request;
        this.importService = importService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void run() {
        try {
            logger.info("Processing import file: {}", file.getAbsoluteFile());
            Identifier identifier = importService.importRequest(file, request);
            if (identifier != null) {
                applicationEventPublisher.publishEvent(new RecordUpdatedEvent(identifier.getRecord()));
                applicationEventPublisher.publishEvent(new IGSNUpdatedEvent(identifier, request));
            }
            logger.info("Processed import file: {}", file.getAbsoluteFile());
        } catch (IOException e) {
            // todo log the exception in the request log
            logger.error(e.getMessage());
        }catch (ForbiddenOperationException e) {
            logger.warn(e.getMessage());
        }

    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }
}
