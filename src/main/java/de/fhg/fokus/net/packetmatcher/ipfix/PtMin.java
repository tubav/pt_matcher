package de.fhg.fokus.net.packetmatcher.ipfix;

import java.math.BigInteger;

import de.fhg.fokus.net.ptapi.ProbeRecord;

/**
 * Minimal record for packet tracking. 
 *  
 * @author FhG-FOKUS NETwork Research
 *
 */
public final class PtMin implements ProbeRecord  {
	private static final long serialVersionUID = 1L;
	/**
	 * Observation Domain Id
	 */
	public long oid;
	public long exportTime;
	private BigInteger observationTimeMicroseconds;
	private final long digestHashValue;
	private final short ipTTL;
	
	public PtMin(long oid, BigInteger observationTimeMicroseconds,
			long digestHashValue, short ipTTL) {
		this.oid = oid;
		this.observationTimeMicroseconds = observationTimeMicroseconds;
		this.digestHashValue = digestHashValue;
		this.ipTTL = ipTTL;
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
		return String.format("{oid: %d, observationTimeMicroseconds:%s, digestHashValue:%d, ipTTL:%d }",
				oid, observationTimeMicroseconds.toString(), digestHashValue, ipTTL);
	}
	@Override
	public String csvData() {
		return String.format("%d, %s, %d, %d",
				oid, observationTimeMicroseconds.toString(), digestHashValue, ipTTL);
	}
	private static final String CSV_FIELDS="oid, observationTimeMicroseconds, digestHashValue, ipTTL ";
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
	private static BigInteger N1000 = BigInteger.valueOf(1000);
	
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
		this.exportTime=exportTime;
	}
	@Override
	public void setObservationTimeMilliseconds(long observationTimeMilliseconds) {
		this.observationTimeMicroseconds= BigInteger.valueOf(observationTimeMilliseconds).multiply(N1000);
	}
	
}
