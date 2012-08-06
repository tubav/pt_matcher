package de.fhg.fokus.net.packetmatcher.ipfix;

import java.math.BigInteger;

import de.fhg.fokus.net.ptapi.ProbeRecord;

/**
 * Ts TTL record for packet tracking. Used by packet matcher.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public final class PtTsTtl implements ProbeRecord {
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

	public PtTsTtl(BigInteger observationTimeMicroseconds,
			long digestHashValue, short ipTTL, int totalLengthIPv4,
			short protocolIdentifier, short ipVersion) {
		this.observationTimeMicroseconds = observationTimeMicroseconds;
		this.digestHashValue = digestHashValue;
		this.ipTTL = ipTTL;
		this.totalLengthIPv4 = totalLengthIPv4;
		this.protocolIdentifier = protocolIdentifier;
		this.ipVersion = ipVersion;
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
		return String
				.format("{oid: %d, exportTime: %d, observationTimeMicroseconds:%s, digestHashValue:%d, ipTTL:%d, "
						+ "totalLengthIPv4: %d, protocolIdentifier: %d, IpVersion %d }",
						oid, exportTime,
						observationTimeMicroseconds.toString(),
						digestHashValue, ipTTL, totalLengthIPv4,
						protocolIdentifier, ipVersion);
	}

	@Override
	public String csvData() {
		return String.format("%d, %d, %s, %d, %d, %d, %d, %d", oid, exportTime,
				observationTimeMicroseconds.toString(), digestHashValue, ipTTL,
				totalLengthIPv4, protocolIdentifier, ipVersion);

	}

	private static final String CSV_FIELDS = "oid, exportTime, observationTimeMicroseconds, "
			+ "digestHashValue, ipTTL, totalLengthIPv4, protocolIdentifier, IpVersion";

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

}
