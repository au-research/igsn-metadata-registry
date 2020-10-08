package au.edu.ardc.registry.igsn.job.tasklet;

import au.edu.ardc.registry.common.entity.Request;
import au.edu.ardc.registry.common.model.Attribute;
import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.FragmentProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import au.edu.ardc.registry.exception.ContentProviderNotFoundException;
import au.edu.ardc.registry.igsn.service.IGSNRequestService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * PayloadChunkerTasklet loads the payload and creates individual record content
 * {position}.{xml/json} for each resource in the feed ready to be imported into the
 * registry
 */
public class PayloadChunkerTasklet implements Tasklet, InitializingBean {

	final SchemaService schemaService;

	final IGSNRequestService igsnRequestService;

	public PayloadChunkerTasklet(SchemaService schemaService, IGSNRequestService igsnRequestService) {
		this.schemaService = schemaService;
		this.igsnRequestService = igsnRequestService;
	}

	/**
	 * @param contribution a contribution to a {@link StepExecution},
	 * @param chunkContext the current context for the tasklet {@link ChunkContext},
	 * @return RepeatStatus {@link RepeatStatus}, since this a single {@link Tasklet} so
	 * it'll return RepeatStatus.FINISHED after 1 iteration
	 * @throws ContentProviderNotFoundException Exceptions if chunker provider for given
	 * content doesn't exist
	 * @throws IOException Exceptions if files cannot be opened or saved
	 */
	@Override
	public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext)
			throws ContentProviderNotFoundException, IOException {

		// obtain the Request
		JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
		String requestID = jobParameters.getString("IGSNServiceRequestID");
		Request request = igsnRequestService.findById(requestID);

		// obtain neccessary parameters from the Request.attributes
		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		String resultDirName = request.getAttribute(Attribute.CHUNKED_PAYLOAD_PATH);

		String payload = Helpers.readFile(payloadPath);
		Schema schema = schemaService.getSchemaForContent(payload);
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
				Metadata.Fragment);
		assert resultDirName != null;
		Files.createDirectories(Paths.get(resultDirName));
		for (int i = 0; i < fragmentProvider.getCount(payload); i++) {
			String content = fragmentProvider.get(payload, i);
			String outFilePath = resultDirName + File.separator + i + fileExtension;
			Helpers.writeFile(outFilePath, content);
		}
		chunkContext.setComplete();
		return RepeatStatus.FINISHED;
	}

	@Override
	public void afterPropertiesSet() {
	}

}
