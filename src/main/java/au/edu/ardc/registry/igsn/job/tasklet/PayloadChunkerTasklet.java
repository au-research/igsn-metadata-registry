package au.edu.ardc.registry.igsn.job.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;

public class PayloadChunkerTasklet implements Tasklet, InitializingBean {

	private Resource directory;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		File dir = directory.getFile();
		Assert.state(dir.isDirectory());
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(directory, "directory must be set");
	}

}
