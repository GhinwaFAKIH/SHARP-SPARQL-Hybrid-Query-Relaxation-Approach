package connor.utils;

import connor.ConnorPartition;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hayats
 *
 * Thread for computiong Concepts of Neighbours
 */
public class ConnorThread extends Thread {
	private final ConnorPartition partition;
	private final AtomicBoolean cut;
	private long execTime;

	public ConnorThread(ConnorPartition partition, AtomicBoolean cut) {
		super();
		this.partition = partition;
		this.cut = cut;
	}

	@Override
	public void run() {
		try {
			this.partition.fullPartitioning(cut);
		} catch(PartitionException | TableException e) {
			e.printStackTrace();
		}
		this.execTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(this.getId());
	}

	public long getExecTime() {
		return execTime;
	}
}
