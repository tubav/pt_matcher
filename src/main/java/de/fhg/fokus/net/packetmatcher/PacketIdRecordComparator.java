package de.fhg.fokus.net.packetmatcher;

import java.util.Comparator;

public class PacketIdRecordComparator implements Comparator<PacketIdRecord> {

	public int compare(PacketIdRecord arg0, PacketIdRecord arg1) {
		/* if TTL is present then we do compare by TTL - else by timestamp */
		if (arg0.getTtl() != 0 ) {
			if (arg0.getTtl() < arg1.getTtl())
				return 1;
			if (arg0.getTtl() > arg1.getTtl())
				return -1;
			if (arg0.getTtl() == arg1.getTtl()) {
				if (arg0.getTimeStamp() < arg1.getTimeStamp())
					return -1;
				if (arg0.getTimeStamp() > arg1.getTimeStamp())
					return 1;
				if (arg0.getTimeStamp() == arg1.getTimeStamp())
					return 1;
			}
		}
		else {
			if (arg0.getTimeStamp() < arg1.getTimeStamp())
				return -1;
			if (arg0.getTimeStamp() == arg1.getTimeStamp())
				return 1;
			if (arg0.getTimeStamp() > arg1.getTimeStamp())
				return 1;
		}
		return -1;
	}
}
