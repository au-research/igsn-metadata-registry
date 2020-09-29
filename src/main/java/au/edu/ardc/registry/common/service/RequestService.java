package au.edu.ardc.registry.common.service;

import au.edu.ardc.registry.common.config.ApplicationProperties;
import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.User;
import au.edu.ardc.registry.common.repository.RequestRepository;
import au.edu.ardc.registry.common.repository.specs.RequestSpecification;
import au.edu.ardc.registry.exception.ForbiddenOperationException;
import au.edu.ardc.registry.exception.RequestNotFoundException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RequestService {

	final RequestRepository requestRepository;

	final ApplicationProperties applicationProperties;

	private Map<String, Logger> loggers;

	public RequestService(RequestRepository requestRepository, ApplicationProperties applicationProperties) {
		this.requestRepository = requestRepository;
		this.applicationProperties = applicationProperties;
		loggers = new HashMap<>();
	}

	/**
	 * Find a request by id, created by a given user
	 * @param id uuid of the request
	 * @param user the creator {@link User}
	 * @return the {@link Request}
	 * @throws RequestNotFoundException when the request is not found
	 * @throws ForbiddenOperationException when the request is not owned by the user
	 */
	public Request findOwnedById(String id, User user) throws RequestNotFoundException, ForbiddenOperationException {
		Optional<Request> opt = requestRepository.findById(UUID.fromString(id));
		Request request = opt.orElseThrow(() -> new RequestNotFoundException(id));

		if (!request.getCreatedBy().equals(user.getId())) {
			throw new ForbiddenOperationException("User does not have access to this request");
		}

		return request;
	}

	/**
	 * Finds a {@link Request} by it's id
	 * @param id the UUID string
	 * @return the {@link Request}
	 */
	public Request findById(String id) {
		Optional<Request> opt = requestRepository.findById(UUID.fromString(id));
		return opt.orElse(null);
	}

	/**
	 * Performs a search based on the predefined search specification
	 * @param specs an instance of {@link RequestSpecification}
	 * @param pageable an instance of {@link Pageable}
	 * @return a {@link Page} of {@link Request}
	 */
	public Page<Request> search(Specification<Request> specs, Pageable pageable) {
		return requestRepository.findAll(specs, pageable);
	}

	/**
	 * Get an instance of {@link Logger} for use with a {@link Request}. The Service will
	 * create a new Logger instance if required, and store them within {@link #loggers}
	 * map, return them as needed
	 * @param request the {@link Request} to create/use a logger from
	 * @return the {@link Logger} instance
	 */
	public Logger getLoggerFor(Request request) {
		String loggerName = getLoggerNameFor(request);
		if (loggers.containsKey(loggerName)) {
			return loggers.get(loggerName);
		}
		Logger logger = createLoggerFor(request);
		loggers.put(loggerName, logger);
		return logger;
	}

	/**
	 * Build a new {@link Logger} instance for the provided {@link Request}
	 * @param request the {@link Request} to create a logger from
	 * @return the {@link Logger} with proper FileAppender
	 */
	private Logger createLoggerFor(Request request) {
		String loggerName = getLoggerNameFor(request);
		String loggerPath = getLoggerPathFor(request);

		// get the current Logging context and Configuration
		LoggerContext context = (LoggerContext) LogManager.getContext(true);
		Configuration configuration = context.getConfiguration();
		LoggerConfig loggerConfig = new LoggerConfig(loggerName, Level.INFO, false);

		// build a PatternLayout to be used with logging
		String pattern = "[%d{ISO8601}][%-5p][%c{2}] %m%n";
		PatternLayout.Builder builder = PatternLayout.newBuilder().withPattern(pattern)
				.withCharset(Charset.defaultCharset()).withAlwaysWriteExceptions(false).withNoConsoleNoAnsi(false);
		PatternLayout layout = builder.build();

		// build the appender and add them to the loggerConfig
		Appender appender = FileAppender.newBuilder().setName(loggerName).setLayout(layout).withFileName(loggerPath)
				.withAppend(true).setConfiguration(configuration).withLocking(false).withImmediateFlush(true)
				.withBufferSize(8192).withAdvertise(false).withAdvertiseUri("").build();
		loggerConfig.addAppender(appender, Level.INFO, null);

		// add a new logger with the provided config
		configuration.addLogger(loggerName, loggerConfig);

		// update all the loggers to make sure this logger by name is available everywhere
		context.updateLoggers();
		return context.getLogger(loggerName);
	}

	/**
	 * Returns the configured logger path for a {@link Request}
	 * @param request the {@link Request}
	 * @return the absolute String path to the log file
	 */
	public String getLoggerPathFor(Request request) {
		return applicationProperties.getDataPath() + "/requests/" + request.getId() + "/logs";
	}

	/**
	 * Returns the name of the {@link Logger} instance for a given {@link Request}
	 * @param request the {@link Request}
	 * @return the String name of the Logger for use globally
	 */
	public String getLoggerNameFor(Request request) {
		return "Request." + request.getId();
	}

	/**
	 * Close the logger by {@link Request}
	 * Use to remove all appenders from the Logger instance and reduce memory footprint
	 * @param request the {@link Request}
	 */
	public void closeLoggerFor(Request request) {
		String loggerName = getLoggerNameFor(request);
		LoggerContext context = (LoggerContext) LogManager.getContext(true);
		Configuration configuration = context.getConfiguration();
		context.getLogger(loggerName).getAppenders()
				.forEach((s, appender) -> context.getLogger(loggerName).removeAppender(appender));
		configuration.removeLogger(loggerName);
		context.updateLoggers();
		loggers.remove(loggerName);
	}

	// todo create
	// todo delete
	// todo update

}
