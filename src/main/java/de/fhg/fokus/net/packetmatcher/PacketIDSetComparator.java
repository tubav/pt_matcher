package de.fhg.fokus.net.packetmatcher;

import java.util.Comparator;
import java.util.SortedSet;

public class PacketIDSetComparator implements Comparator<SortedSet<PacketIdRecord>> {

	@Override
	public int compare(SortedSet<PacketIdRecord> o1,
			SortedSet<PacketIdRecord> o2) {
		if (o1.first().getTimeStamp() < o1.first().getTimeStamp()) return -1;
		if (o1.first().getTimeStamp() > o1.first().getTimeStamp()) return 1;
		return -1;
	}

}
