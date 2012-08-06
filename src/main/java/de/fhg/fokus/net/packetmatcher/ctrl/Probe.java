package de.fhg.fokus.net.packetmatcher.ctrl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.ipfix.api.IpfixConnectionHandler;
import de.fhg.fokus.net.ptapi.PtInterfaceStats;
import de.fhg.fokus.net.ptapi.PtProbeStats;

/**
 * Models a probe 
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public final class Probe {
	// -- constants --
	/**
	 * Probe connection will be dropped in case of too many unknown sets.
	 * In this case template records have not been send or could not be decoded.
	 */
	private static final short MAX_NUMBER_OF_UNKNOWN_SETS = 20;
	private final static String NOT_AVAILABLE = "\"not available\"";

	// -- sys --
	private final static AtomicInteger atomicConsoleId = new AtomicInteger(1);
	private final static Logger logger = LoggerFactory.getLogger(Probe.class);
	// -- model --
	private final IpfixConnectionHandler ipfixConnectionHandler;
	private final BufferedWriter cmdOut;
	// -- model.stats --
	private final int sessionId;
	private long probeStatsRecords =0;
	private long samplingStatsRecords = 0;
	private long pktIdRecords = 0;
	private long templateRecords = 0;
	private long syncResponseRecords = 0;
	private long numberOfUnknownSets = 0;
	private String latitude = "";
	private String longitude = "";
	private long maxNumberOfUnknownSets = MAX_NUMBER_OF_UNKNOWN_SETS;
	private final long connectedTs;
	private PtProbeStats lastProbeStatsRecord;
	private Map<String, PtInterfaceStats> interfaceStatsMaps = new ConcurrentHashMap<String, PtInterfaceStats>();

	public IpfixConnectionHandler getIpfixConnectionHandler() {
		return ipfixConnectionHandler;
	}
	
	public PtProbeStats getLastProbeStatsRecord() {
		return lastProbeStatsRecord;
	}
	
	public void setLocation(String latitude, String longitude){
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public void setLastProbeStatsRecord(PtProbeStats lastProbeStatsRecord) {
		this.lastProbeStatsRecord = lastProbeStatsRecord;
	}
	
	public void setLastInterfaceStatsRecord(PtInterfaceStats lastSamplingStatsRecord) {
		if( lastSamplingStatsRecord!=null){
			interfaceStatsMaps.put(lastSamplingStatsRecord.getInterfaceName(), lastSamplingStatsRecord);
		}
	}
	
	public void incUnknownSets( short unknownsets ){
		this.numberOfUnknownSets+= unknownsets;
		if(numberOfUnknownSets>maxNumberOfUnknownSets){
			try {
				logger.warn("Too many unknown sets from probe received ({}), dropping {}",
						numberOfUnknownSets,
						ipfixConnectionHandler.getSocket().getRemoteSocketAddress());
				ipfixConnectionHandler.getSocket().close();
			} catch (IOException e) {
				logger.debug(e.getMessage());
			}
		}
	}
	
	/**
	 * Attained Selection Fraction
	 */
	public Probe(IpfixConnectionHandler handler) throws IOException {
		this.ipfixConnectionHandler = handler;
		this.connectedTs = System.currentTimeMillis();
		this.sessionId = atomicConsoleId.getAndAdd(1);
		this.cmdOut = new BufferedWriter(new OutputStreamWriter(ipfixConnectionHandler.getSocket().getOutputStream(),"ascii"));
	}
	
	public String formatLatitude(String latitude){
		try{
			double lat = Double.parseDouble(latitude);
			char direction = 'N';
			int degree = (int) lat;
			double decimal;
			int minutes;
			int seconds;
			if (degree < 0){
				degree = degree*-1;
				direction = 'S';
				decimal = (lat + degree)*-1;
				minutes = (int)(decimal*60);
				seconds = (int)((decimal*60-minutes)*60);
			}
			else{
				decimal = lat - degree;
				minutes = (int)(decimal*60);
				seconds = (int)((decimal*60+minutes)*60);
			}
			return degree + "d " + minutes + "m " + seconds + "s " + direction;
		}
		catch(NumberFormatException e){
			return "No Location received!";
		}
	}
	
	public String formatLongitude(String longitude){
		try{
			double lon = Double.parseDouble(longitude);
			char direction = 'E';
			int degree = (int) lon;
			double decimal;
			int minutes;
			int seconds;
			if (degree < 0){
				degree = degree*-1;
				direction = 'W';
				decimal = (lon + degree)*-1;
				minutes = (int)(decimal*60);
				seconds = (int)((decimal*60-minutes)*60);
			}
			else{
				decimal = lon - degree;
				minutes = (int)(decimal*60);
				seconds = (int)((decimal*60+minutes)*60);
			}
			return degree + "d " + minutes + "m " + seconds + "s " + direction;
		}
		catch(NumberFormatException e){
			return "No Location received!";
		}
	}

	@Override
	public String toString() {
		long secs = (System.currentTimeMillis() - connectedTs)/1000;
		String lat = formatLatitude(latitude);
		String lon = formatLongitude(longitude);
		return String.format(Locale.ENGLISH,"p%d:{ from:\"%s\", pktids:%d, conn: \"%d s\", msgs: %d, lat: %s, long: %s } ",
				sessionId,ipfixConnectionHandler.getSocket().getRemoteSocketAddress(),
				pktIdRecords, secs,ipfixConnectionHandler.totalReceivedMessages(), lat, lon);
	}

	private String getProbeStats(){
		if(lastProbeStatsRecord==null){
			return NOT_AVAILABLE;
		}
		return String.format(Locale.ENGLISH,
				"{ oid: %d, observationTimeMilliseconds: %d, \n" +
				"     systemCpuIdle: %f, systemMemFree: %d, \n" +
				"     processCpuUser: %f, processCpuSys: %f,\n" +
				"     processMemVzs: %d, processMemRss: %d  \n"+"" +
				"}",
				lastProbeStatsRecord.oid, lastProbeStatsRecord.observationTimeMilliseconds, 
				lastProbeStatsRecord.systemCpuIdle, lastProbeStatsRecord.systemMemFree, lastProbeStatsRecord.processCpuUser,
				lastProbeStatsRecord.processCpuSys, lastProbeStatsRecord.processMemVzs, lastProbeStatsRecord.processMemRss );

	}
	
	private String getInterfaceStats(PtInterfaceStats ifstats){
		if(ifstats==null){
			return NOT_AVAILABLE;
		}
		double asf = -1.0;
		double delta = ifstats.getPacketDeltaCount().doubleValue();
		if( delta > 0){
			asf = ifstats.getSamplingSize() / delta;
		}
		return String.format(Locale.ENGLISH,
				"{oid: %d, observationTimeMilliseconds: %d,\n" +
				"     samplingSize: %d, packetDeltaCount: %s\n " +
				"     pcapStatDrop: %d, pcapStatRecv: %d,\n" +
				"     interfaceName: \"%s\", iterfaceDescription: \"%s\", \n" +
				"     asf: %.2f \n" +
				"},",
				ifstats.oid,
				ifstats.observationTimeMilliseconds,
				ifstats.samplingSize,
				ifstats.packetDeltaCount.toString(),
				ifstats.pcapStatDrop,
				ifstats.pcapStatRecv,
				ifstats.interfaceName,
				ifstats.interfaceDescription,
				asf);
	}
	public String getStats(){
		long secs = (System.currentTimeMillis() - connectedTs)/1000;
		StringBuffer sbuf = new StringBuffer();
		for (Entry<String, PtInterfaceStats>  e : interfaceStatsMaps.entrySet()) {
			sbuf.append(e.getKey());
			sbuf.append(":");
			sbuf.append(getInterfaceStats(e.getValue()));
			sbuf.append("\n");
		}
		
		return String.format(Locale.ENGLISH,
				"p%d:{ \n" +
				"  remote: \"%s\", connTimeSecs: %d,\n" +
				"  recs: { tmpl:%d, pktid:%d, probeStat:%d, samplingStat:%d }   \n"+
				"  unknownSets: %d, \n"+
				"  probeStats: %s \n" +
				"  interfaceStats: \n" +
				"%s\n" +
				"}", 
				sessionId,
				ipfixConnectionHandler.getSocket().getRemoteSocketAddress(),
				secs,
				templateRecords,
				pktIdRecords,probeStatsRecords,samplingStatsRecords,
				numberOfUnknownSets,
				getProbeStats(), sbuf.toString()
		);
	}
	public String getConsoleId() {
		return "p"+sessionId;
	}
	public void incSamplingStatsRecords() {
		samplingStatsRecords++;

	}
	public void incProbeStatsRecords(){
		probeStatsRecords++;
	}
	public void incSyncResponseRecords(){
		syncResponseRecords++;
	}
	public void incPktIdRecords() {
		pktIdRecords++;
	}
	public void incTemplateRecords() {
		templateRecords++;
	}
	public void setSamplingRate( double rate ){
		sendCmd(String.format(Locale.ENGLISH,"-r %f\n", rate));
	}
	/**
	 * Send command to a probe
	 * @param cmd
	 * @return true in case of success
	 */
	public boolean sendCmd( String cmd ){
		try {
			cmdOut.write(cmd);
			cmdOut.write("\n");
			cmdOut.flush();
			return true;
		} catch (IOException e) {
			logger.error("could not send cmd:\"{}\" due to:",cmd,e.getMessage());
			return false;
		}
		
	}

	public long getProbeStatsRecords() {
		return probeStatsRecords;
	}

	public void setProbeStatsRecords(long probeStatsRecords) {
		this.probeStatsRecords = probeStatsRecords;
	}

	public long getSamplingStatsRecords() {
		return samplingStatsRecords;
	}

	public void setSamplingStatsRecords(long samplingStatsRecords) {
		this.samplingStatsRecords = samplingStatsRecords;
	}

	public long getPktIdRecords() {
		return pktIdRecords;
	}

	public void setPktIdRecords(long pktIdRecords) {
		this.pktIdRecords = pktIdRecords;
	}

	public long getTemplateRecords() {
		return templateRecords;
	}

	public void setTemplateRecords(long templateRecords) {
		this.templateRecords = templateRecords;
	}

	public long getSyncResponseRecords() {
		return syncResponseRecords;
	}

	public void setSyncResponseRecords(long syncResponseRecords) {
		this.syncResponseRecords = syncResponseRecords;
	}

	public long getNumberOfUnknownSets() {
		return numberOfUnknownSets;
	}

	public void setNumberOfUnknownSets(long numberOfUnknownSets) {
		this.numberOfUnknownSets = numberOfUnknownSets;
	}

	public int getSessionId() {
		return sessionId;
	}

	public long getConnectedTs() {
		return connectedTs;
	}
	


}
