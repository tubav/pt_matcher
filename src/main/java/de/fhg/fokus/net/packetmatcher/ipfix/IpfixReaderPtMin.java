package de.fhg.fokus.net.packetmatcher.ipfix;

import java.nio.ByteBuffer;

import de.fhg.fokus.net.ipfix.api.IpfixDataRecord;
import de.fhg.fokus.net.ipfix.api.IpfixDataRecordReader;
import de.fhg.fokus.net.ipfix.api.IpfixMessage;
import de.fhg.fokus.net.ipfix.api.IpfixTemplateForDataReader;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeDigestHashValue;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeIpTTL;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeObservationTimeMicroseconds;

/**
 * Minimal IPFIX record reader for packet tracking.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public final class IpfixReaderPtMin implements IpfixDataRecordReader {
	private final IpfixIeObservationTimeMicroseconds ie1 = new IpfixIeObservationTimeMicroseconds(
			8);
	private final IpfixIeDigestHashValue ie2 = new IpfixIeDigestHashValue(4);
	private final IpfixIeIpTTL ie3 = new IpfixIeIpTTL(1);
	private final IpfixTemplateForDataReader template = new IpfixTemplateForDataReader(
			ie1, ie2, ie3);

	@Override
	public Object getRecord(IpfixMessage msg, ByteBuffer setBuffer) {
		if (!setBuffer.hasRemaining()) {
			return null;
		}
		
		return new PtMin(
				msg.getObservationDomainID(), 
				ie1.getBigInteger(setBuffer), 
				ie2.getLong(setBuffer),
				ie3.getShort(setBuffer)
				);
	}

	public String toString() {
		return "PtMinimal";

	};

	@Override
	public IpfixTemplateForDataReader getTemplate() {
		return template;
	}

	@Override
	public Object getRecord(IpfixMessage msg, IpfixDataRecord rec) {
		return new PtMin(
				msg.getObservationDomainID(), 
				ie1.getBigInteger(rec.getByteBuffer(ie1.getFieldSpecifier())), 
				ie2.getLong(rec.getByteBuffer(ie2.getFieldSpecifier())),
				ie3.getShort(rec.getByteBuffer(ie3.getFieldSpecifier()))
				);
	}

}
