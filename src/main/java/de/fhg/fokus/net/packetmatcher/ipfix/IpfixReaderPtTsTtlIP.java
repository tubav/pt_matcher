package de.fhg.fokus.net.packetmatcher.ipfix;

import java.nio.ByteBuffer;

import de.fhg.fokus.net.ipfix.api.IpfixDataRecord;
import de.fhg.fokus.net.ipfix.api.IpfixDataRecordReader;
import de.fhg.fokus.net.ipfix.api.IpfixMessage;
import de.fhg.fokus.net.ipfix.api.IpfixTemplateForDataReader;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeDigestHashValue;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeIpTTL;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeIpVersion;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeObservationTimeMicroseconds;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeProtocolIdentifier;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeTotalLengthIPv4;

import de.fhg.fokus.net.ipfix.model.ie.IpfixIeSourceIPv4Address;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeSourceTransportPort;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeDestinationIPv4Address;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeDestinationTransportPort;

//import java.net.Inet4Address;

/**
 * Minimal IPFIX record for packet tracking.
 *
 * @author FhG-FOKUS NETwork Research
 *
 */
public final class IpfixReaderPtTsTtlIP implements IpfixDataRecordReader {
	private final IpfixIeObservationTimeMicroseconds ie1 = new IpfixIeObservationTimeMicroseconds(8);
	private final IpfixIeDigestHashValue ie2 = new IpfixIeDigestHashValue(4);
	private final IpfixIeIpTTL ie3 = new IpfixIeIpTTL(1);
	private final IpfixIeTotalLengthIPv4 ie4 = new IpfixIeTotalLengthIPv4(2);
	private final IpfixIeProtocolIdentifier ie5 = new IpfixIeProtocolIdentifier(1);
	private final IpfixIeIpVersion ie6 = new IpfixIeIpVersion(1);
	private final IpfixIeSourceIPv4Address ie7 = new IpfixIeSourceIPv4Address(4);
	private final IpfixIeSourceTransportPort ie8 = new IpfixIeSourceTransportPort(2);
	private final IpfixIeDestinationIPv4Address ie9 = new IpfixIeDestinationIPv4Address(4);
	private final IpfixIeDestinationTransportPort ie10 = new IpfixIeDestinationTransportPort(2);
	
	private final IpfixTemplateForDataReader template = new IpfixTemplateForDataReader(
			ie1, ie2, ie3, ie4, ie5, ie6, ie7, ie8, ie9, ie10);

	@Override
	public Object getRecord(IpfixMessage msg, ByteBuffer setBuffer) {
		if (!setBuffer.hasRemaining()) {
			return null;
		}
		return new PtTsTtlIP(
				ie1.getBigInteger(setBuffer),
				ie2.getLong(setBuffer), 
				ie3.getShort(setBuffer),
				ie4.getInt(setBuffer), 
				ie5.getShort(setBuffer),
				ie6.getShort(setBuffer),
				ie7.getAddress(setBuffer),
				ie8.getInt(setBuffer),
				ie9.getAddress(setBuffer),
				ie10.getInt(setBuffer)
				);
	}

	public String toString() {
		return "PtTsTtl";
	};

	@Override
	public IpfixTemplateForDataReader getTemplate() {
		return template;
	}

	@Override
	public Object getRecord(IpfixMessage msg, IpfixDataRecord rec) {
		return new PtTsTtlIP(
				ie1.getBigInteger(rec.getByteBuffer(ie1.getFieldSpecifier())),
				ie2.getLong(rec.getByteBuffer(ie2.getFieldSpecifier())), 
				ie3.getShort(rec.getByteBuffer(ie3.getFieldSpecifier())),
				ie4.getInt(rec.getByteBuffer(ie4.getFieldSpecifier())), 
				ie5.getShort(rec.getByteBuffer(ie5.getFieldSpecifier())),
				ie6.getShort(rec.getByteBuffer(ie6.getFieldSpecifier())),
				ie7.getAddress(rec.getByteBuffer(ie7.getFieldSpecifier())),
				ie8.getInt(rec.getByteBuffer(ie8.getFieldSpecifier())),
				ie9.getAddress(rec.getByteBuffer(ie9.getFieldSpecifier())),
				ie10.getInt(rec.getByteBuffer(ie10.getFieldSpecifier()))
				);
	}

}
