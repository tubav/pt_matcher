package de.fhg.fokus.net.packetmatcher;

import java.util.SortedSet;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class MatchingTask extends TimerTask {
	ConcurrentSkipListSet<PacketIdRecord> pktidRecords; 
	private final BlockingQueue<SortedSet<PacketIdRecord>> queue;
	
	public MatchingTask(ConcurrentSkipListSet<PacketIdRecord> pktidRecords, BlockingQueue<SortedSet<PacketIdRecord>> queue) {
		this.pktidRecords = pktidRecords;
		this.queue = queue;
	}

	@Override
	public void run() {
		try {
			// System.out.println("TimeOut: recordSize = " + pktidRecords.size());
			if (pktidRecords.size() > 1) {
				queue.put(pktidRecords);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
};
