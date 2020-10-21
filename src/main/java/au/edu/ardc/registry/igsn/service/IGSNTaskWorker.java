package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.igsn.model.IGSNTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class IGSNTaskWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(IGSNTaskWorker.class);

	private BlockingQueue<IGSNTask> queue;

	private final IGSNService igsnService;

	public IGSNTaskWorker(BlockingQueue<IGSNTask> queue, IGSNService igsnService) {
		this.queue = queue;
		this.igsnService = igsnService;
	}

	@Override
	public void run() {
		logger.info("Started worker thread {}", Thread.currentThread().getId());
		try {
			while (true) {
				logger.info("Try to do a job");
				IGSNTask task = queue.take();
				if (task.getType().equals("DONE")) {
					logger.info("DONE message received, shutting down thread {}", Thread.currentThread().getId());
					break;
				}
				igsnService.executeTask(task);
				logger.info("Finish doing a job");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
