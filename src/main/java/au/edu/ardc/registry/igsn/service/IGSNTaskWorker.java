package au.edu.ardc.registry.igsn.service;

import au.edu.ardc.registry.igsn.model.IGSNTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class IGSNTaskWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(IGSNTaskWorker.class);

	public BlockingQueue<IGSNTask> queue;

	private final IGSNService igsnService;

	public IGSNTaskWorker(BlockingQueue<IGSNTask> queue, IGSNService igsnService) {
		this.queue = queue;
		this.igsnService = igsnService;
	}

	/**
	 * Run the {@link IGSNTask} that is in the provided {@link BlockingQueue}
	 */
	@Override
	public void run() {
		logger.info("Started worker thread {}", Thread.currentThread().getId());
		try {
			while (true) {
				IGSNTask task = queue.take();
				logger.debug("Running Task: {}", task);

				// poison pill method of killing the thread, by feeding it DONE
				if (task.getType().equals("DONE")) {
					logger.info("DONE message received, shutting down thread {}", Thread.currentThread().getId());
					break;
				}

				// execute the IGSNTask and then move on to the next
				igsnService.executeTask(task);
			}
		}
		catch (InterruptedException e) {
			logger.error("Worker Thread {} is Interrupted. Reason: {}", Thread.currentThread().getId(), e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

}
