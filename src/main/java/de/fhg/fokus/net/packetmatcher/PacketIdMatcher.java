package de.fhg.fokus.net.packetmatcher;

import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


public class PacketIdMatcher {
	private final Map<Integer, ConcurrentSkipListSet<PacketIdRecord>> pktIdMap = new ConcurrentHashMap<Integer, ConcurrentSkipListSet<PacketIdRecord>>();
	@SuppressWarnings("unused")
	private static final long serialVersionUID = -5308076544397098015L;
	private long timeout;
	private BlockingQueue<SortedSet<PacketIdRecord>> exportqueue;
	private final Timer timer = new Timer();
	
	public PacketIdMatcher() {
	}

	public PacketIdMatcher(int matching_timeout_ms, int matching_period, BlockingQueue<SortedSet<PacketIdRecord>> queue) {
		this.timeout = matching_timeout_ms;
		this.exportqueue = queue;
	}

	public void addPacketIdRecord(PacketIdRecord pir) {
		ConcurrentSkipListSet<PacketIdRecord> tmpSet = pktIdMap.get(pir.getPacketID());

		if (tmpSet == null) {
			ConcurrentSkipListSet<PacketIdRecord> newSet = new ConcurrentSkipListSet<PacketIdRecord>( new PacketIdRecordComparator());
			newSet.add(pir);
			pktIdMap.put(pir.getPacketID(), newSet);
			timer.schedule(new MatchingTask(newSet, exportqueue), timeout);
		} else {
			tmpSet.add(pir);			
		}
	}

//	public synchronized void evalMatchingPacketSets(long now) {
//		// System.out.println("time: " + (long) now);
//		for (SortedSet<PacketIdRecord> set : pktIdMap.values()) {
//			if (set.size() <= 1 )
//				continue;
//
//			// TODO check for gaps and (possibly) "move" only a subset of records in this set into the vector v
//			// export found set in the vector
//			
//			if (set.first().getTimeStamp()> now + this.timeout ) {
//				PacketTrackRecord record = new PacketTrackRecord(set.size());
//				int i=0;
//				for (PacketIdRecord pir : set) {
//					record.probeIds[i] = pir.getProbeID();
//					record.ts[i] = pir.getTimeStamp();
//					i++;
//				}
//				
//			}		
//
//			// delete the set from the hashmap (this)
//			pktIdMap.remove(set.first().getPacketID());
//
//		}
//
//	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public Map<Integer, ConcurrentSkipListSet<PacketIdRecord>> getPktIdMap() {
		return pktIdMap;
	}
}
