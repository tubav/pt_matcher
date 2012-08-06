package de.fhg.fokus.net.packetmatcher.ipfix;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.ipfix.api.IpfixDataRecord;
import de.fhg.fokus.net.ipfix.api.IpfixDataRecordReader;
import de.fhg.fokus.net.ipfix.api.IpfixMessage;
import de.fhg.fokus.net.ipfix.api.IpfixTemplateForDataReader;
import de.fhg.fokus.net.ipfix.model.ie.IpfixIeObservationTimeMilliseconds;
import de.fhg.fokus.net.packetmatcher.ipfix.ie.IpfixIePtMessage;
import de.fhg.fokus.net.packetmatcher.ipfix.ie.IpfixIePtMessageId;
import de.fhg.fokus.net.packetmatcher.ipfix.ie.IpfixIePtMessageValue;

/**
 * Minimal IPFIX record reader for packet tracking.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public final class IpfixReaderPtSync implements IpfixDataRecordReader {
	private final static Logger logger = LoggerFactory
			.getLogger(IpfixReaderPtSync.class);
	private final IpfixIeObservationTimeMilliseconds ie1 = new IpfixIeObservationTimeMilliseconds(
			8);
	private final IpfixIePtMessageId ie2 = new IpfixIePtMessageId(4);
	private final IpfixIePtMessageValue ie3 = new IpfixIePtMessageValue(4);
	private final IpfixIePtMessage ie4 = new IpfixIePtMessage();
	private final IpfixTemplateForDataReader template = new IpfixTemplateForDataReader(
			ie1, ie2, ie3, ie4);

	@Override
	public Object getRecord(IpfixMessage msg, ByteBuffer setBuffer) {
		if (!setBuffer.hasRemaining()) {
			return null;
		}
		long ie1_d = ie1.getBigInteger(setBuffer).longValue();
		long ie2_d = ie2.getLong(setBuffer);
		long ie3_d = ie3.getLong(setBuffer);

		String ie4_d = "";
		try {
			ie4_d = ie4.getString(setBuffer, "ascii");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		}
		return new PtSync(msg.getObservationDomainID(), ie1_d, ie2_d, ie3_d,
				ie4_d);
	}

	public String toString() {
		return "PtSync";

	};

	@Override
	public IpfixTemplateForDataReader getTemplate() {
		return template;
	}

	@Override
	public Object getRecord(IpfixMessage msg, IpfixDataRecord rec) {
		String ie4_d = "";
		try {
			ie4_d = ie4.getString(rec.getByteBuffer(ie4.getFieldSpecifier()), "ascii");
		} 
		catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		}
		return new PtSync(
				msg.getObservationDomainID(), 
				ie1.getBigInteger(rec.getByteBuffer(ie1.getFieldSpecifier())).longValue(),
				ie2.getLong(rec.getByteBuffer(ie2.getFieldSpecifier())),
				ie3.getLong(rec.getByteBuffer(ie3.getFieldSpecifier())),
				ie4_d
				);
	}

}
