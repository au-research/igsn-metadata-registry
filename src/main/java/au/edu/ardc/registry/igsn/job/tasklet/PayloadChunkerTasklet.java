package au.edu.ardc.registry.igsn.job.tasklet;

import au.edu.ardc.registry.common.model.Schema;
import au.edu.ardc.registry.common.provider.FragmentProvider;
import au.edu.ardc.registry.common.provider.IdentifierProvider;
import au.edu.ardc.registry.common.provider.Metadata;
import au.edu.ardc.registry.common.provider.MetadataProviderFactory;
import au.edu.ardc.registry.common.service.SchemaService;
import au.edu.ardc.registry.common.util.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PayloadChunkerTasklet loads the payload and creates individual record content
 * {position}.{xml/json} for each resource in the feed ready to be imported into the
 * registry
 */
public class PayloadChunkerTasklet implements Tasklet, InitializingBean {

	private String directory;

	private String payloadPath;

	private SchemaService service;

	private Map<String, String> taskInfo;

	/**
	 * @param contribution a contribution to a {@link StepExecution},
	 * @param chunkContext
	 * @return
	 * @throws Exception
	 */
	@Override
	public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext)
			throws Exception {
		File dir = new File(directory);
		String payload = Helpers.readFile(payloadPath);

		Schema schema = this.service.getSchemaForContent(payload);
		String fileExtension = Helpers.getFileExtensionForContent(payload);
		FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
		IdentifierProvider iProvider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		taskInfo = new HashMap<>();
		assert fProvider != null;
		int numberOfFragments = fProvider.getCount(payload);
		for (int i = 0; i < numberOfFragments; i++) {
			String content = fProvider.get(payload, i);
			assert iProvider != null;
			String outFilePath = directory + File.separator + i + fileExtension;
			Helpers.writeFile(outFilePath, content);
			String identifier = iProvider.get(content);
			taskInfo.put(outFilePath, identifier);
		}
		saveTaskInfo();
		chunkContext.setComplete();
		return RepeatStatus.FINISHED;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(directory, "directory must be set");
	}

	/**
	 * saveTaskInfo
	 * @throws IOException if taskInfo file can not be saved
	 */
	private void saveTaskInfo() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File(directory + File.separator + getClass().getSimpleName() + "_taskInfo.json"),
				taskInfo);
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public void setSchemaService(SchemaService service) {
		this.service = service;
	}

	public void setPayloadPath(String payloadPath) {
		this.payloadPath = payloadPath;
	}

}
