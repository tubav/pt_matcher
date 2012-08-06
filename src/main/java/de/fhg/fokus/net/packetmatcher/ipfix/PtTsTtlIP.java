package de.fhg.fokus.net.packetmatcher.ipfix;

import java.math.BigInteger;

import de.fhg.fokus.net.ptapi.ProbeRecord;

import java.net.Inet4Address;

/**
 * Ts TTL record for packet tracking. Used by packet matcher.
 *
 * @author FhG-FOKUS NETwork Research
 *
 */
public final class PtTsTtlIP implements ProbeRecord {
	private static final long serialVersionUID = 1L;
	/**
	 * Observation Domain Id
	 */
	public long oid;
	private long exportTime;
	private BigInteger observationTimeMicroseconds;
	private final long digestHashValue;
	private final short ipTTL;
	private final int totalLengthIPv4;
	private final short protocolIdentifier;
	private final short ipVersion;
        public final Inet4Address sourceAddress;
        public final int sourcePort;
        public final Inet4Address destinationAddress;
        public final int destinationPort;

	public PtTsTtlIP(BigInteger observationTimeMicroseconds,
			long digestHashValue, short ipTTL, int totalLengthIPv4,
			short protocolIdentifier, short ipVersion,
                        Inet4Address sourceAddress,
                        int sourcePort,
                        Inet4Address destinationAddress,
                        int destinationPort) {

		this.observationTimeMicroseconds = observationTimeMicroseconds;
		this.digestHashValue = digestHashValue;
		this.ipTTL = ipTTL;
		this.totalLengthIPv4 = totalLengthIPv4;
		this.protocolIdentifier = protocolIdentifier;
		this.ipVersion = ipVersion;

                this.sourceAddress = sourceAddress;
                this.sourcePort = sourcePort;
                this.destinationAddress = destinationAddress;
                this.destinationPort = destinationPort;
	}

	public int getTotalLengthIPv4() {
		return totalLengthIPv4;
	}

	public short getProtocolIdentifier() {
		return protocolIdentifier;
	}

	public short getIpVersion() {
		return ipVersion;
	}

	public BigInteger getObservationTimeMicroseconds() {
		return observationTimeMicroseconds;
	}

	public long getDigestHashValue() {
		return digestHashValue;
	}

	public short getIpTTL() {
		return ipTTL;
	}

	@Override
	public String toString() {
		return String.format("{oid: %d, exportTime: %d, observationTimeMicroseconds: %d, digestHashValue:%d, ipTTL:%d, "
						+ "totalLengthIPv4: %d, protocolIdentifier: %d, IpVersion %d " +
                                                " sourceAddress: %d, sourcePort: %d, destinationAddress: %d, destinationPort: %d}",
						oid, exportTime,
						observationTimeMicroseconds,
						digestHashValue, ipTTL, totalLengthIPv4,
						protocolIdentifier, ipVersion,
                                                sourceAddress, sourcePort, destinationAddress, destinationPort);
	}

	@Override
	public String csvData() {
		return String.format("%d, %d, %s, %d, %d, %d, %d, %d, %d, %d, %d, %d", oid, exportTime,
				observationTimeMicroseconds, digestHashValue, ipTTL,
				totalLengthIPv4, protocolIdentifier, ipVersion, sourceAddress, sourcePort,
                                destinationAddress, destinationPort);

	}

	private static final String CSV_FIELDS = "oid, exportTime, observationTimeMicroseconds, "
			+ "digestHashValue, ipTTL, totalLengthIPv4, protocolIdentifier, IpVersion" +
                        " ,sourceAddress, sourcePort, destinationAddress, destinationPort";

	@Override
	public String csvFields() {
		return CSV_FIELDS;
	}

	@Override
	public long getOid() {
		return oid;
	}

	@Override
	public long getExportTime() {
		return exportTime;
	}

	private static final BigInteger N1000 = BigInteger.valueOf(1000);

	@Override
	public long getObservationTimeMilliseconds() {
		return observationTimeMicroseconds.divide(N1000).longValue();
	}

	@Override
	public void setOid(long oid) {
		this.oid = oid;
	}

	@Override
	public void setExportTime(long exportTime) {
		this.exportTime = exportTime;
	}

	@Override
	public void setObservationTimeMilliseconds(long observationTimeMilliseconds) {
		this.observationTimeMicroseconds = BigInteger.valueOf(
				observationTimeMilliseconds).multiply(N1000);
	}

        public Inet4Address getDestinationAddress() {
                return this.destinationAddress;
        }

        public int getDestinationPort() {
                return this.destinationPort;
        }

        public Inet4Address getSourceAddress() {
                return this.sourceAddress;
        }

        public int getSourcePort() {
                return this.sourcePort;
        }

}
