package de.fhg.fokus.net.packetmatcher;
import java.net.Inet4Address;


public class PacketIdRecord {

	private int packetID = 0;
	private long timeStamp = 0;
	private int probeID = 0;
	private int packetSize = 0;
	private int ttl = 0;
	private byte version = 0;
	private int protocol = 0;
        private Inet4Address sourceAddress;
        private int sourcePort = 0;
        private Inet4Address destinationAddress;
        private int destinationPort = 0;
	
	public PacketIdRecord() {
		
	}
	
	public PacketIdRecord(int packetID, long timeStamp, int probeID,
			int packetSize, int ttl) {
		this.packetID = packetID;
		this.timeStamp = timeStamp;
		this.probeID = probeID;
		this.packetSize = packetSize;
		this.ttl = ttl;
	}

        public PacketIdRecord(int packetID, long timeStamp, int probeID,
			int packetSize, int ttl, Inet4Address sourceAddress, int sourcePort,
                        Inet4Address destinationAddress, int destinationPort) {
                this.packetID = packetID;
		this.timeStamp = timeStamp;
		this.probeID = probeID;
		this.packetSize = packetSize;
		this.ttl = ttl;
                this.sourceAddress = sourceAddress;
                this.sourcePort = sourcePort;
                this.destinationAddress = destinationAddress;
                this.destinationPort = destinationPort;
        }

		
	public int getPacketSize() {
		return packetSize;
	}

	public int getTtl() {
		return ttl;
	}

	public byte getVersion() {
		return version;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public int getPacketID() {
		return packetID;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public int getProbeID() {
		return probeID;
	}

	public void setPacketID(int packetID) {
		this.packetID = packetID;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public void setProbeID(int probeID) {
		this.probeID = probeID;
	}
        
        public Inet4Address getSourceAddress() {
                return sourceAddress;
        }
        
        public void setSourceAddress(Inet4Address sourceAddress) {
                this.sourceAddress = sourceAddress;
        }

        public int getSourcePort() {
                return sourcePort;
        }
        
        public void setSourcePort(int sourcePort) {
            this.sourcePort = sourcePort;
        }

        public Inet4Address getDestinationAddress() {
                return destinationAddress;
        }

        public void setDestinationAddress(Inet4Address destinationAddress) {
                this.destinationAddress = destinationAddress;
        }

        public int getDestinationPort() {
                return destinationPort;
        }

        public void setDestinationPort(int destinationPort) {
                this.destinationPort = destinationPort;
        }

	public String toString() {
		return "packetID: " + String.valueOf(packetID) + " sourceID " + String.valueOf(probeID) + " timestamp:" + timeStamp;
	}

}
