package de.fhg.fokus.net.packetmatcher;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.ipfix.IpfixCollector;
import de.fhg.fokus.net.ipfix.api.IpfixCollectorListener;
import de.fhg.fokus.net.ipfix.api.IpfixConnectionHandler;
import de.fhg.fokus.net.ipfix.api.IpfixMessage;
import de.fhg.fokus.net.ipfix.api.IpfixSet;
import de.fhg.fokus.net.ipfix.api.IpfixTemplateRecord;
import de.fhg.fokus.net.ipfix.util.HexDump;
import de.fhg.fokus.net.packetmatcher.ctrl.Console;
import de.fhg.fokus.net.packetmatcher.ctrl.Probe;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtProbeLocation;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtInterfaceStats;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtMin;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtProbeStats;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtSync;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtTsTtl;
import de.fhg.fokus.net.packetmatcher.ipfix.IpfixReaderPtTsTtlIP;
import de.fhg.fokus.net.packetmatcher.ipfix.PtMin;
import de.fhg.fokus.net.packetmatcher.ipfix.PtSync;
import de.fhg.fokus.net.packetmatcher.ipfix.PtTsTtl;
import de.fhg.fokus.net.packetmatcher.ipfix.PtTsTtlIP;
import de.fhg.fokus.net.ptapi.ProbeRecord;
import de.fhg.fokus.net.ptapi.PtInterfaceStats;
import de.fhg.fokus.net.ptapi.PtProbeLocation;
import de.fhg.fokus.net.ptapi.PtProbeStats;

/**
 * Packet Matcher
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class Matcher {
	// -- constants --
	public static final String VERSION = "v.2010-11";

	// -- config --
	
	@Option(name="-exportHost",usage="host to send packet records to (localhost)")
	private String exportHost = "localhost";
	
	@Option(name="-exportPort", usage="export port (40123)")
	private int exportPort = 40123;
	
	@Option(name="-exportReconnectInterval",usage="export reconnect interval in second (7)")
	private int exportReconnectTimeSeconds = 7;
	
	@Option(name="-consolePort", usage="console port (4000)")
	private int consolePort = 4000;

	@Option(name="-listenPort", usage="IPFIX listen port (4739)")
	private int listenPort = 4739;

	@Option(name="-ttlcheck", usage="perform ttl checks by matching (no)")
	private boolean ttlcheck = false;

	@Option(name="-verbose", usage="verbose log output (no)")
	private boolean verbose = false;
	
	@Option(name="-csv", usage="export data to csv files (no)")
	private boolean fileExportEnableCsv = false;
	
	@Option(name="-obj", usage="export data to java obj files (no)")
	private boolean fileExportEnableObj = false;
	
	@Option(name="-exportFolder", usage="export Folder (matcher_data)")
	private String fileExportFolder = "matcher_data";
	
	// -- sys --
	private static final Logger logger = LoggerFactory.getLogger(Matcher.class);

	// -- model --
	private final IpfixCollector ipfixCollector = new IpfixCollector();
	private int matchingTimeoutMillis = 5000;
	private int matchingPeriodMillis = 5000;

	// TODO add support for stopping/starting export task
	private ExportTask exportTask;
	private PacketIdMatcher packetIdMatcher;
	
	private int initialQueueSize = 20000;
	
	private final FileExporter fileExporter = new FileExporter();

	private interface RecordHandler {
		public void onRecord(IpfixMessage msg, Probe probe, Object rec);
	}

	private Map<Class<?>, RecordHandler> recordHandlerMap = new HashMap<Class<?>, RecordHandler>();

	// -- probe sync --
	private  Console consoleSync;

	/**
	 * initialize a shutdown hook which is doing a clean shut down
	 */
	public Matcher() {
		Runtime.getRuntime().addShutdownHook( new Thread ( "ShutdownHook" ){
			@Override
			public void run() {
				System.err.println();
				logger.info("shutting down..");
				
				// shut down matcher
				Matcher.this.shutdown();
				
				logger.info("bye");
			}
		});
	}

	public void shutdown(){
		fileExporter.stop();
		
		if(consoleSync!=null){
			consoleSync.shutdown();
		}
		
		try {
			if(exportTask!=null){
				exportTask.close();
			}
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}

	/**
	 * Depending on the record received various actions are taken. These actions
	 * are implemented via callbacks defined in this function. Dispatching
	 * callbacks is done via recordHandlerMap.
	 */
	private void setupRecordHandlers() {
		// Probe location
		recordHandlerMap.put(PtProbeLocation.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				PtProbeLocation r = (PtProbeLocation) rec;
				/*
				logger.debug("-------------------------------------------------");
				logger.debug("Observation Time: " + r.getObservationTimeMilliseconds());
				logger.debug("Source IPv4: " + r.getSourceIpv4Address());
				logger.debug("Latitude: " + r.getLatitude());
				logger.debug("Longitude: " + r.getLongitude());
				logger.debug("Probe Name: " + r.getProbeName());
				logger.debug("Probe Location Name: " + r.getProbeLocationName());
				logger.debug("-------------------------------------------------");
				*/
				if (probe != null) {
					probe.setLocation(r.getLatitude(),r.getLongitude());
				}
				if (!exportTask.putProbeRecord(r)) {
					logger.warn("export queue not ready, dropping record: {}", rec);
				}
			}
		});

		// Pktid records
		// - create PacketIdRecord and
		// - forward it to matcher
		recordHandlerMap.put(PtMin.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				PtMin r = (PtMin) rec;
				if (probe != null) {
					probe.incPktIdRecords();
				}
				// TODO add support to packet size
				PacketIdRecord pir = new PacketIdRecord(
						(int) r.getDigestHashValue(), 
						r.getObservationTimeMicroseconds().longValue(),
						(int) msg.getObservationDomainID(), 
						0, 
						r.getIpTTL());
				packetIdMatcher.addPacketIdRecord(pir);

			}
		});

		// Pktid records
		// - create PacketIdRecord with IP-Addresses and Ports and
		// - forward it to matcher
		recordHandlerMap.put(PtTsTtl.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				PtTsTtl r = (PtTsTtl) rec;
				if (probe != null) {
					probe.incPktIdRecords();
				}
				PacketIdRecord pir = new PacketIdRecord((int) r
						.getDigestHashValue(), r
						.getObservationTimeMicroseconds().longValue(),
						(int) msg.getObservationDomainID(), r.getTotalLengthIPv4(), r.getIpTTL());
				pir.setVersion((byte)r.getIpVersion());
				pir.setProtocol(r.getProtocolIdentifier());
				packetIdMatcher.addPacketIdRecord(pir);
			}
		});

		recordHandlerMap.put(PtTsTtlIP.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				PtTsTtlIP r = (PtTsTtlIP) rec;
				if (probe != null) {
					probe.incPktIdRecords();
				}
				// TODO add support to packet size
				PacketIdRecord pir = new PacketIdRecord(
						(int) r.getDigestHashValue(),
						r.getObservationTimeMicroseconds().longValue(),
						(int) msg.getObservationDomainID(),
						r.getTotalLengthIPv4(),
						r.getIpTTL(),
						r.sourceAddress,
						r.sourcePort,
						r.destinationAddress,
						r.destinationPort);
				pir.setVersion((byte)r.getIpVersion());
				pir.setProtocol(r.getProtocolIdentifier());
				packetIdMatcher.addPacketIdRecord(pir);
			}
		});

		// Probe statistics
		// - forward to output queue
		recordHandlerMap.put(PtProbeStats.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				PtProbeStats r = (PtProbeStats) rec;
				if (probe != null) {
					probe.incProbeStatsRecords();
					probe.setLastProbeStatsRecord(r);
				}

				if (!exportTask.putProbeRecord(r)) {
					logger.warn("export queue not ready, dropping record: {}", rec);
				}
			}
		});

		// Sampling statistics
		// - forward to output queue
		recordHandlerMap.put(PtInterfaceStats.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				PtInterfaceStats r = (PtInterfaceStats) rec;
				if (probe != null) {
					probe.setLastInterfaceStatsRecord(r);
				}
				if (!exportTask.putProbeRecord(r)) {
					logger.warn("export queue not ready, dropping record: {}", rec);
				}
			}
		});

		// Template records
		// - add to statistics
		recordHandlerMap.put(IpfixTemplateRecord.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				if (probe != null) {
					logger.info("receive template record: {}", rec);
					probe.incTemplateRecords();
				}
			}
		});

		// PTSync responses
		//
		recordHandlerMap.put(PtSync.class, new RecordHandler() {
			@Override
			public void onRecord(IpfixMessage msg, Probe probe, Object rec) {
				consoleSync.receive(probe, (PtSync) rec);
				if (probe != null) {
					probe.incSyncResponseRecords();
				}
			}
		});
	}

	private void init() throws UnknownHostException, IOException {
		BlockingQueue<SortedSet<PacketIdRecord>> queue = 
				new ArrayBlockingQueue<SortedSet<PacketIdRecord>>(initialQueueSize);

		packetIdMatcher = new PacketIdMatcher(matchingTimeoutMillis,
				matchingPeriodMillis, queue);

		exportTask = new ExportTask(this.fileExporter, queue, InetAddress.getByName(exportHost),
				exportPort, packetIdMatcher.getPktIdMap());
	}

	public void startFileExporter(){
		if(fileExportEnableCsv||fileExportEnableObj){
			fileExporter.setEnableCsv(fileExportEnableCsv);
			fileExporter.setEnableObj(fileExportEnableObj);
			fileExporter.start(new File(fileExportFolder));
		}
	}

	public void stopFileExporter(){
		fileExporter.stop();
	}

	/**
	 * start matcher
	 * 
	 * @return
	 * @throws IOException
	 */
	public Matcher setupIpfixCollector() throws IOException {
		
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtMin());
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtProbeStats());
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtInterfaceStats());
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtTsTtl());
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtSync());
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtProbeLocation());
		ipfixCollector.registerDataRecordReader(new IpfixReaderPtTsTtlIP());
		
		logger.debug("adding EventListener ... ");
		
		ipfixCollector.addEventListener(new IpfixCollectorListener() {
			@Override
			public void onMessage(IpfixConnectionHandler handler, IpfixMessage msg) {
				try {
					if (msg == null) {
						logger.warn("Strange, msg is null, this might be a bug in the IPFIX collector");
						return;
					}
					Probe probe = (Probe) handler.getAttachment();
					if (probe == null) { // this should never happen
						logger.warn("onMessage dispatched before onConnect, you may lose some probe statistics");
					}
					// reading records and dispatching record handlers
					// TODO: make this debug
					logger.info("-- Message received: {}", msg);
					for (IpfixSet set : msg) {
						logger.info(" +- Message Process Set: {}", set);
						for (Object rec : set) {
							logger.info("  +- Message Process record: {}", rec);
							RecordHandler recordHandler = recordHandlerMap.get(rec.getClass());
							if (recordHandler != null) {
								if( verbose ){
									logger.debug("No record handler for: {}", rec);
								}
								// This might have been set in the respective data record reader,
								// let's assure it here anyway
								if (rec instanceof ProbeRecord) {
									ProbeRecord prec = (ProbeRecord) rec;
									prec.setOid(msg.getObservationDomainID());
									prec.setExportTime(msg.getExportTime());
									fileExporter.putReceivedRecord(prec);
								}
								recordHandler.onRecord(msg, probe, rec);
							}
							else {
								logger.warn("unknown record received:{}, {}"
											, rec.getClass().getSimpleName()
											, rec);
							}
						}
					}
					if (probe != null) {
						probe.incUnknownSets(msg.getNumberOfunknownSets());
					}
				} catch (Exception e) {
					logger.debug(e.toString());
					logger.debug(e.getMessage());
					logger.debug(HexDump.toHexString(msg.getMessageBuffer()));
//					try {
//						handler.getSocket().close();
//					} catch (IOException e1) {
//						e1.printStackTrace();
//					}
					
				}
			}
			
			@Override
			public void onConnect(IpfixConnectionHandler handler) {
				logger.debug("handler: {}", handler.hashCode());
				Probe probe;
				try {
					probe = new Probe(handler);
				} catch (IOException e) {
					logger.warn(
							"Could not initialize probe, dropping connection: {}",
							handler.getSocket().getRemoteSocketAddress());
					logger.debug(e.getMessage());
					try {
						handler.getSocket().close();
					} catch (IOException e1) {
						logger.debug(e.getMessage());
					}
					return;
				}
				handler.setAttachment(probe);
				consoleSync.addProbe(probe);
				logger.debug("Exporter CONNECTED from {} ", 
						handler.getSocket().getRemoteSocketAddress());
			}
			
			@Override
			public void onDisconnect(IpfixConnectionHandler handler) {
				Probe probe = (Probe) handler.getAttachment();
				if (probe != null) {
					consoleSync.removeProbe(probe);
					
				}
				else {
					logger.warn("strange, attachment should not be null.");
				}
				
				logger.debug("Exporter DISCONNECTED from {}", handler
						.getSocket().getRemoteSocketAddress());
			}
		});
		
		return this;
	}
	
	// used by args4j
	public void run(){
		System.out.println("starting Packet Matcher, logging to matcher_debug.log");
		logger.info("=== Matcher ===");
		try {
			// setup record handler
			this.setupRecordHandlers();
			
			// setup and start console
			this.consoleSync = new Console(this,consolePort);
			// activate additional stats log output during execution
			this.consoleSync.setVerbose(verbose);
			this.consoleSync.start();
			
			// init matcher
			this.startFileExporter();
			this.init();
			System.out.println(toString());
			
			// start the matcher 
			this.setupIpfixCollector();
			// start the ipfix collector
			ipfixCollector.setServicePort(listenPort);
			ipfixCollector.start();
			
			// start the export task
			exportTask.start(exportReconnectTimeSeconds);
			
			// FIXME review
			if(ttlcheck){
				exportTask.setTtlcheck(ttlcheck);
			}
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}

	public String toString() {
		return String.format(
				"Matcher {\n" +
				"\texport:      \"%s:%d\"\n" +
				"\tconsole:     \"localhost:%d\"\n" +
				"\tttlcheck:     %s\n" +
				"\tverbose:      %s\n" +
				"\tExportTask:   %s\n" +
				"\tFileExporter: %s\n" +
				"} ",
				exportHost, exportPort, 
				consolePort,
				ttlcheck, 
				verbose,
				exportTask.toString(), 
				fileExporter.toString() 
				);
	}

	// -- main --
	public static void main(String[] args) throws IOException {
		logger.info("== Packet Matcher ==");
		Matcher matcher = new Matcher();
		CmdLineParser parser = new CmdLineParser(matcher);
		try {
			parser.parseArgument(args);
			matcher.run();
		}
		catch( CmdLineException cle ) {
			//Handling wrong arguments
			System.err.println(cle.getMessage());
			System.err.print(Matcher.class.getName());
			if (Starter.hasAnnotation(Matcher.class, Option.class)) System.err.print(" [options]");
			if (Starter.hasAnnotation(Matcher.class, Argument.class)) System.err.print(" arguments");
			System.err.println();

			if (parser != null)
				parser.printUsage(System.err);
		}
	}

	// ------------------------------------------------------------------------
	// --- getter and setter --------------------------------------------------
	// ------------------------------------------------------------------------
	public int getInitialQueueSize() {
		return initialQueueSize;
	}

	/**
	 * @param initialQueueSize
	 *            initial size of internal queue used for matching packets.
	 */
	public Matcher setInitialQueueSize(int initialQueueSize) {
		this.initialQueueSize = initialQueueSize;
		return this;
	}

	public int getMatchingTimeoutMillis() {
		return matchingTimeoutMillis;
	}
	
	public Matcher setMatchingTimeoutMillis(int matchingTimeoutMillis) {
		this.matchingTimeoutMillis = matchingTimeoutMillis;
		return this;
	}
	
	public int getMatchingPeriodMillis() {
		return matchingPeriodMillis;
	}
	
	public Matcher setMatchingPeriodMillis(int matchingPeriodMillis) {
		this.matchingPeriodMillis = matchingPeriodMillis;
		return this;
	}
	
	public int getExportPort() {
		return exportPort;
	}
	
	/**
	 * @param exportPort
	 *            port GUI is listening to
	 * @return
	 */
	public Matcher setExportPort(int exportPort) {
		this.exportPort = exportPort;
		return this;
	}
	
	public String getExportHost() {
		return exportHost;
	}
	
	/**
	 * @param exportHost
	 *            host where GUI is running
	 */
	public Matcher setExportHost(String exportHost) {
		this.exportHost = exportHost;
		return this;
	}

	public IpfixCollector getIpfixCollector() {
		return ipfixCollector;
	}

	public FileExporter getFileExporter() {
		return fileExporter;
	}

	public ExportTask getExportTask() {
		return exportTask;
	}

	public void setExportTask(ExportTask exportTask) {
		this.exportTask = exportTask;
	}
}
