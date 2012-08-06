package de.fhg.fokus.net.packetmatcher.ipfix;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

//import java.math.BigInteger;

import de.fhg.fokus.net.ipfix.api.IpfixDataRecord;
import de.fhg.fokus.net.ipfix.api.IpfixIe;
import de.fhg.fokus.net.ipfix.api.IpfixDataRecordReader;
import de.fhg.fokus.net.ipfix.api.IpfixMessage;
import de.fhg.fokus.net.ipfix.api.IpfixTemplateForDataReader;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeLatitude;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeLongitude;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeObservationTimeMilliseconds;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeProbeLocationName;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeProbeName;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeSourceIPv4Address;
import de.fhg.fokus.net.ptapi.PtProbeLocation;
import java.net.Inet4Address;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author FhG-FOKUS NETwork Research
 */
public final class IpfixReaderPtProbeLocation implements IpfixDataRecordReader{
	private final IpfixIeObservationTimeMilliseconds ie1 = new IpfixIeObservationTimeMilliseconds(8);
	private final IpfixIeSourceIPv4Address ie2 = new IpfixIeSourceIPv4Address(4);
	private final IpfixIeLatitude ie3 = new IpfixIeLatitude(IpfixIe.VARIABLE_LENGTH);
	private final IpfixIeLongitude ie4 = new IpfixIeLongitude(IpfixIe.VARIABLE_LENGTH);
	private final IpfixIeProbeName ie5 = new IpfixIeProbeName(IpfixIe.VARIABLE_LENGTH);
	private final IpfixIeProbeLocationName ie6 = new IpfixIeProbeLocationName(IpfixIe.VARIABLE_LENGTH);
	private final IpfixTemplateForDataReader template = new IpfixTemplateForDataReader(
			ie1, ie2, ie3, ie4, ie5, ie6);

	@Override
	public Object getRecord(IpfixMessage msg, ByteBuffer setBuffer) {
		if (!setBuffer.hasRemaining()) {
			return null;
		}

		// Observation Time Milliseconds
		long observationTimeMilliseconds = ie1.getBigInteger(setBuffer).longValue();
		
		// Source Ipv4 Address
		Inet4Address ipv4SourceAddess = ie2.getAddress(setBuffer);
		
		// Latitude
		String latitude = "undefined";
		try {
			latitude = ie3.getString(setBuffer);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// Longitude
		String longitude = "undefined";
		try {
			longitude = ie4.getString(setBuffer);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// Probe Name
		String probeName = "undefined";
		try {
			probeName = ie5.getString(setBuffer);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// Probe Location Name
		String probeLocationName = "undefined";
		try {
			probeLocationName = ie6.getString(setBuffer);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		return new PtProbeLocation(msg.getObservationDomainID(),
				observationTimeMilliseconds,
				ipv4SourceAddess,
				latitude,
				longitude,
				probeName,
				probeLocationName
				);
	}

	@Override
	public String toString() {
		return "PtLocation";
	};

	@Override
	public IpfixTemplateForDataReader getTemplate() {
		return template;
	}

	@Override
	public Object getRecord(IpfixMessage msg, IpfixDataRecord rec) {
		// Observation Time Milliseconds
		long observationTimeMilliseconds = ie1.getBigInteger(rec.getByteBuffer(ie1.getFieldSpecifier())).longValue();
		
		// Source Ipv4 Address
		Inet4Address ipv4SourceAddess = ie2.getAddress(rec.getByteBuffer(ie2.getFieldSpecifier()));
		
		// Latitude
		String latitude = "undefined";
		try {
			latitude = ie3.getString(rec.getByteBuffer(ie3.getFieldSpecifier()));
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// Longitude
		String longitude = "undefined";
		try {
			longitude = ie4.getString(rec.getByteBuffer(ie4.getFieldSpecifier()));
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// Probe Name
		String probeName = "undefined";
		try {
			probeName = ie5.getString(rec.getByteBuffer(ie5.getFieldSpecifier()));
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// Probe Location Name
		String probeLocationName = "undefined";
		try {
			probeLocationName = ie6.getString(rec.getByteBuffer(ie6.getFieldSpecifier()));
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(IpfixReaderPtProbeLocation.class.getName()).log(Level.SEVERE, null, ex);
		}
		return new PtProbeLocation(msg.getObservationDomainID(),
				observationTimeMilliseconds,
				ipv4SourceAddess,
				latitude,
				longitude,
				probeName,
				probeLocationName
				);
	}
}
