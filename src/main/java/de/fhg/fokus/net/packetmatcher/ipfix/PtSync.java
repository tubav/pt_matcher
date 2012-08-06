package de.fhg.fokus.net.packetmatcher.ipfix;

import java.util.Locale;

import de.fhg.fokus.net.ptapi.ProbeRecord;

/**
 * Synchronization responses
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */

public class PtSync implements ProbeRecord {
	private static final long serialVersionUID = 1L;
	/**
	 * Observation Domain Id. Note that if you are using ipfix4java as transport
	 * you'll currently need to get this value from the ipfix message;
	 */
	private long oid;
	private long exportTime;

	/**
	 * The time the record was created. Its encoded according to
	 * dateTimeMilliseconds(RFC5101). The data type dateTimeMilliseconds
	 * represents a time value in units of milliseconds normalized to the GMT
	 * timezone. It contains the number of milliseconds since 0000 UTC Jan 1,
	 * 1970.
	 */
	private long observationTimeMilliseconds;
	/**
	 * Message id
	 */
	private final long messageId;

	private final long messageValue;
	private final String message;

	public PtSync(long oid, long observationTimeMilliseconds, long messageId,
			long messageValue, String message) {
		super();
		this.oid = oid;
		this.messageId = messageId;
		this.observationTimeMilliseconds = observationTimeMilliseconds;
		this.messageValue = messageValue;
		this.message = message;
	}

	@Override
	public String toString() {
		return String.format(Locale.ENGLISH,
				"{ oid: %d, observationTimeMilliseconds: %d, "
						+ "messageId: %d, messageValue: %d, message: \"%s\" }",
				oid, observationTimeMilliseconds, messageId, messageValue,
				message);
	}

	public long getOid() {
		return oid;
	}

	public long getMessageId() {
		return messageId;
	}

	@Override
	public long getObservationTimeMilliseconds() {
		return observationTimeMilliseconds;
	}

	public long getValue() {
		return messageValue;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String csvData() {
		String msg = message == null ? "" : message.replaceAll(",", "_");
		return String.format(Locale.ENGLISH, "%d, %d, %d, %d, %s", oid,
				observationTimeMilliseconds, messageId, messageValue, msg);
	}

	private static final String CSV_FIELDS = "oid, exportTime, observationTimeMilliseconds, messageId, "
			+ "messageValue, message";

	@Override
	public String csvFields() {
		return CSV_FIELDS;
	}

	@Override
	public long getExportTime() {
		return exportTime;
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
		this.observationTimeMilliseconds = observationTimeMilliseconds;
	}

}
