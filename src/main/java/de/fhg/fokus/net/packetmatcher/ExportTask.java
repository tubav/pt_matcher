package de.fhg.fokus.net.packetmatcher;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.ptapi.BaseRecord;
import de.fhg.fokus.net.ptapi.PacketTrackRecord;
import de.fhg.fokus.net.ptapi.ProbeRecord;

public class ExportTask {
	// -- sys --
	private static final Logger logger = LoggerFactory.getLogger(ExportTask.class);
	public Executor pool = Executors.newFixedThreadPool(7);

	// export task should be rewritten, for the meanwhile let's make
	// get the file exporter from matcher
	private final FileExporter fileExporter;
	
	// -- model --
	private Map<Integer, ConcurrentSkipListSet<PacketIdRecord>> pktIdMap;

	/**
	 * Timeout to switch between export queues. The export task will way this time
	 * for an element to be available in the queue. If an element were not available
	 * after this time, the next export queue will be checked. Currently there are 
	 * two export queues: exportqueue, and exportqueueStats.
	 * 
	 */
	private long exportQueuePoolTimeoutMillis = 200;

	private BlockingQueue<SortedSet<PacketIdRecord>> exportQueue;

	/**
	 * Used for relaying stats and sampling records from to
	 * netview. 
	 */
	private final BlockingQueue<ProbeRecord> exportQueueProbeRecords = new LinkedBlockingDeque<ProbeRecord>();
	private ObjectOutput out;
	private Socket client;
	private final InetSocketAddress remoteSocketAddr;
	private Boolean ttlcheck=false;

	private long numberOfStatsRecordsSent = 0;
	private long numberOfTracRecordsSent = 0;

	// -- util --
	private JenkinsHash hashBOBCalculator = new JenkinsHash();

	@Override
	public String toString() {
		if(exportQueue==null){
			return "ExportTask: { \"not yet initialized\"}";
		}
		return String.format(
				"{ " +
				"exportQueue{ size: %d, recs: %d }, " +
				"exportQueueStats{ size: %d, recs: %d }" +
				" }",
				exportQueue.size(),
				numberOfTracRecordsSent,
				exportQueueProbeRecords.size(),
				numberOfStatsRecordsSent
				);
	}
	
	private PacketTrackRecord getRecord(SortedSet<PacketIdRecord> set) {

		int i = 0;
		PacketTrackRecord record = null;

		if (set != null) {
			// System.out.println("Set-Size:" + set.size() + " packetID: ");
			// System.out.println( set.first().getPacketID() + " \n" );
			// record.content.packetLength = set.first().getPacketSize();
			// record.content.version = set.first().getVersion();
			// record.content.protocol = set.first().getProtocol();

			/*
			 * in case there are multiple observations of one packet at one
			 * observationPoint we will drop the second observation so there
			 * will be no weird packet reordering due to bad time synchronisation -
			 * there is only the ordering problem with the first observation which 
			 * will only be seen once with the same TTL as the second observation 
			 * and in case pcap does not observe all packets twice
			 */

			ArrayList<Integer> nodeSet = new ArrayList<Integer>();
			PacketIdRecord probablefirstobservationpoint = set.first();
			int current_ttl = probablefirstobservationpoint.getTtl();
			for (PacketIdRecord pir : set) {
				if (!nodeSet.contains(pir.getProbeID())) {
					nodeSet.add(pir.getProbeID());
					i++;
				}
				if (ttlcheck) { 
					if (pir.getTtl() < (current_ttl-1)) {
						logger.debug("TTL decreased more than 1 - record dropped");
						return null;
					}
				}
				else {
					current_ttl = pir.getTtl();
				}
			}

			record = new PacketTrackRecord(i);
			ByteBuffer bBuffer = ByteBuffer.allocate(4 * i);
			nodeSet.clear();

			i=0;
			for (PacketIdRecord pir : set) {

				if (!nodeSet.contains(pir.getProbeID())) {
					nodeSet.add(pir.getProbeID());
					record.oids[i] = pir.getProbeID();
					record.ts[i] = pir.getTimeStamp();
					record.ttl[i] = pir.getTtl();
					record.size = pir.getPacketSize();
					// Setting source and destination addresses / ports
					record.sourceAddress = pir.getSourceAddress();
					record.sourcePort = pir.getSourcePort();
					record.destinationAddress = pir.getDestinationAddress();
					record.destinationPort = pir.getDestinationPort();
					
					bBuffer.putInt(pir.getProbeID());
					i++;
				} else {
					if ((pir.getProbeID() == probablefirstobservationpoint.getProbeID()) && (i == 2) ) {
						/**
						 *  there may be a timestamp reordering problem between first and second observation
						 *  example A (ts1,ttl64)-B (ts2,ttl64)-A (ts3,ttl63) - isin a wrong order - it must be B,A,A 
						 *  we just exchange A and B and give B the earlier timestamp ...  
						 */
						record.oids[0] = record.oids[1];
						record.oids[1] = pir.getProbeID();
					}
				}
				record.trackid = (int) hashBOBCalculator.hash(bBuffer.array());
				record.pktid = set.first().getPacketID();
			}
		}
		return record;

	}

	/**
	 * Create an export task
	 * 
	 * @param exportqueue
	 * @param iaddr
	 * @param portnumber
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ExportTask(FileExporter fileExporter, BlockingQueue<SortedSet<PacketIdRecord>> exportqueue,
			InetAddress iaddr, int portnumber, Map<Integer, ConcurrentSkipListSet<PacketIdRecord>> pktIdMap) {
		this.fileExporter = fileExporter;
		this.exportQueue = exportqueue;
		this.remoteSocketAddr = new InetSocketAddress(iaddr, portnumber);
		this.pktIdMap = pktIdMap;
	}

	/**
	 * Connect to collector
	 * 
	 * @return
	 * @throws IOException
	 */
	public ExportTask start(int timeout) throws IOException {
		logger.debug("Starting export task");
		
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				reconnect();
			}
		}, 0, timeout, TimeUnit.SECONDS);
		
		return this;
	}
	
	private void reconnect() {
		if (null == this.client || !this.client.isConnected()) {
			try {
				this.client = new Socket(remoteSocketAddr.getAddress(), remoteSocketAddr.getPort());
				if (this.client.isConnected()) {
					logger.debug("ExportTask connected to {}", this.client.getRemoteSocketAddress());
				}
				
				// create an output stream for this socket
				this.out = new ObjectOutputStream(this.client.getOutputStream());
				
				// start processing
				logger.debug("starting queue handling task");
				pool.execute(new Runnable() {
					public void run() {
						handlequeues();
					}
				});
			} 
			catch (IOException e) {
				logger.error("ExportTask: could not connect to: {}", getRemoteSocketAddr());
			}
		}
	}

	private void handlequeues() {
		SortedSet<PacketIdRecord> set = null;
		int count = 0;
		try {
			while (true) {
				if( count < 20 && exportQueue.size() > 0 ) {
					set = exportQueue.poll(exportQueuePoolTimeoutMillis, TimeUnit.MILLISECONDS);
					count++;
					PacketTrackRecord record = getRecord(set);
					if (record != null) {
						if(record.oids.length > 1) {
//							logger.debug("export record {}", record.toString() );
							out.writeObject(record);
							out.flush();
							numberOfTracRecordsSent++;
							fileExporter.putSentRecord(record);
						}
					}
					pktIdMap.remove(record.pktid);
				} else {
					count=0;
					BaseRecord stats = exportQueueProbeRecords.poll(exportQueuePoolTimeoutMillis, TimeUnit.MILLISECONDS);
					if(stats != null){
						out.writeObject(stats);
						out.flush();
						numberOfStatsRecordsSent++;
						fileExporter.putSentRecord(stats);
						continue;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.info("finshed connection");
			client =null;
		}
	}

	public InetSocketAddress getRemoteSocketAddr() {
		return remoteSocketAddr;
	}

	public Socket getClient() {
		return client;
	}

	public void setTtlcheck(Boolean ttlcheck) {
		this.ttlcheck = ttlcheck;
	}

	public void close() throws IOException {
		if(out!=null ){
			out.close();
		}
		if(client!=null){
			client.close();
		}
	}

	public long getExportQueuePoolTimeoutMillis() {
		return exportQueuePoolTimeoutMillis;
	}

	public void setExportQueuePoolTimeoutMillis(long exportQueuePoolTimeoutMillis) {
		this.exportQueuePoolTimeoutMillis = exportQueuePoolTimeoutMillis;
	}
	public boolean putProbeRecord( ProbeRecord prec ){
		return exportQueueProbeRecords.offer(prec);
	}

}
