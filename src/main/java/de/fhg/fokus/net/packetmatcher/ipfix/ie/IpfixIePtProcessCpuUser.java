package de.fhg.fokus.net.packetmatcher.ipfix.ie;
// === ipfix-model-generator: auto-generated file - do not edit! ===
import de.fhg.fokus.net.ipfix.api.IpfixFieldSpecifier;
import de.fhg.fokus.net.ipfix.api.IpfixIe;
import de.fhg.fokus.net.ipfix.api.IpfixIeSemantics;
import de.fhg.fokus.net.ipfix.api.IpfixIeStatus;
import de.fhg.fokus.net.ipfix.api.IpfixIeUnits;
import de.fhg.fokus.net.ipfix.api.codec.IpfixIeCodecFloat32;



/**
 * <pre>
PtProcessCpuUser:{ 
  elementId:342, 
  dataType:float32, 
  dataTypeSemantis:quantity, 
  units:null 
  status:current 
  en: 12325 
}
</pre> 
 * 
 */
public final class IpfixIePtProcessCpuUser extends IpfixIeCodecFloat32 implements IpfixIe {
	// -- model --
	private final IpfixFieldSpecifier fieldSpecifier;

	@Override
	public IpfixFieldSpecifier getFieldSpecifier() {
		return fieldSpecifier;
	}

	public IpfixIePtProcessCpuUser() {
		this.fieldSpecifier = new IpfixFieldSpecifier(12325).setId(342)
				.setFieldLength(this.fieldLength);
	}
	public IpfixIePtProcessCpuUser( int length ) {
		this.fieldLength = length;
		this.fieldSpecifier = new IpfixFieldSpecifier(12325).setId(342)
				.setFieldLength(this.fieldLength);
	}
	public IpfixIePtProcessCpuUser( int length, long enterpriseNumber, boolean isScope ) {
		this.fieldLength = length;
		this.fieldSpecifier = new IpfixFieldSpecifier(enterpriseNumber).setId(342)
				.setFieldLength(this.fieldLength).setScope(isScope);
	}


	@Override
	public IpfixIeSemantics getSemantics() {
		return IpfixIeSemantics.QUANTITY;
	}

	@Override
	public IpfixIeStatus getStatus() {
		return IpfixIeStatus.CURRENT;
	}

	@Override
	public String getName() {
		return "PtProcessCpuUser";
	}

	@Override
	public int getLength() {
		return fieldSpecifier.getIeLength();
	}

	@Override
	public IpfixIeUnits getUnits() {
		return IpfixIeUnits.NONE;
	}
}
