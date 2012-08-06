package de.fhg.fokus.net.packetmatcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.ptapi.BaseRecord;

/**
 * The FileExporter is responsible for writing measurement and export data to
 * files. There are two types of data. The data the matcher receives (via
 * IPFIX4Java) and the data it sends (to netview) .
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class FileExporter {
	// -- constants --
	private static final int QUEUE_SIZE = 10000;
	private static final int QUEUE_TIMEOUT_MILLIS = 300;
	// -- sys --
	public final Executor executor = Executors.newCachedThreadPool();
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// -- model --
	private final AtomicLong counter = new AtomicLong(0);
	private File exportFolder = new File("matcher_data");
	private boolean enableCsv = false;
	private boolean enableObj = false;
	/**
	 * 
	 */
	private boolean stopped = true;

	public boolean isStopped() {
		return stopped;
	}

	// represents the set of export data
	// there are currently two:
	// - received: data matcher receives
	// - sent: data matcher sents
	private class ExportSet {
		final String name;
		File outputDir;
		ObjectOutputStream oos;
		final BlockingQueue<BaseRecord> queue = new ArrayBlockingQueue<BaseRecord>(
				QUEUE_SIZE);
		ConcurrentMap<Class<?>, BufferedWriter> csvWriterMap = new ConcurrentHashMap<Class<?>, BufferedWriter>();

		public ExportSet(String name) {
			this.name = name;
		}
	}

	private final ExportSet received = new ExportSet("received");
	private final ExportSet sent = new ExportSet("sent");
	private ExportSet group[] = { received, sent };

	// data folder format
	private final SimpleDateFormat dateFmtFolder = new SimpleDateFormat(
			"yyyyMMdd'_'HHmmss");
	// file header timestamp format
	private final SimpleDateFormat dateFmtHeader = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.S");

	/**
	 * Insert a record into "received" queue.
	 * 
	 * @param rec
	 */
	public void putReceivedRecord(BaseRecord rec) {
		if (stopped) {
			return;
		}
		if (rec == null) {
			logger.warn("received a null record, ignoring it.");
			return;
		}
		if (!received.queue.offer(rec)) {
			logger.warn("queue full, dropping: {}", rec.csvData());
		}
	}

	/**
	 * Insert a recort into "sent" queue.
	 * 
	 * @param rec
	 */
	public void putSentRecord(BaseRecord rec) {
		if (stopped) {
			return;
		}
		if (rec == null) {
			logger.warn("received a null record, ignoring it.");
			return;
		}
		if (!sent.queue.offer(rec)) {
			logger.warn("queue full, dropping: {}", rec.csvData());
		}
	}

	private void export(final ExportSet es) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if (enableObj) {
					File file = new File(es.outputDir.getAbsolutePath()
							+ File.separator + es.name + ".obj");
					try {
						es.oos = new ObjectOutputStream(new FileOutputStream(
								file));
					} catch (IOException e) {
						logger.error(e.getMessage());
					}

				}
				try {
					logger.info("Exporting data to {}" + File.separator,
							es.outputDir.getAbsolutePath());
					while (!stopped) {
						// release block from time to time so we can
						// quit the loop
						BaseRecord rec = es.queue.poll(QUEUE_TIMEOUT_MILLIS,
								TimeUnit.MILLISECONDS);
						if (rec == null) {
							continue;
						}
						if (enableObj && es.oos != null) {
							es.oos.writeObject(rec);
							es.oos.flush();
						}
						if (enableCsv) {
							BufferedWriter csv = es.csvWriterMap.get(rec
									.getClass());
							if (csv == null) {
								File file = new File(es.outputDir
										.getAbsolutePath()
										+ File.separator
										+ rec.getClass().getSimpleName()
										+ ".csv");
								csv = new BufferedWriter(new FileWriter(file,
										true));
								// saving writer for future reference
								es.csvWriterMap.put(rec.getClass(), csv);
								// write header
								csv.write("# "
										+ dateFmtHeader.format(new Date()));
								csv.newLine();
								csv.write("# "
										+ rec.getClass().getCanonicalName());
								csv.newLine();
								csv.write("# record_id, ");
								csv.write(rec.csvFields());
								csv.newLine();
							}
							csv.write(counter.addAndGet(1) + ", ");
							csv.write(rec.csvData());
							csv.newLine();
							csv.flush();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getMessage());
					FileExporter.this.stop();
				}
			}
		});
	}

	public FileExporter() {

	}

	public void start() {
		start(exportFolder);
	}

	public void start(File exportFolder) {
		this.exportFolder = exportFolder;
		this.stopped = false;
		String folder = dateFmtFolder.format(new Date());

		for (ExportSet es : group) {
			es.outputDir = new File(exportFolder.getAbsolutePath()
					+ File.separator + folder + File.separator + es.name);
			if (!es.outputDir.mkdirs()) {
				throw new RuntimeException("could not create export folder "
						+ es.outputDir.getAbsoluteFile());
			}
			// start writing thread
			export(es);
		}
	}

	public void stop() {
		if (stopped) {
			return;
		}
		logger.info("stopping file exporter.");
		for (ExportSet es : group) {
			for (BufferedWriter wr : es.csvWriterMap.values()) {
				try {
					wr.close();
				} catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
			try {
				if (es.oos != null) {
					es.oos.close();
				}
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
			es.csvWriterMap.clear();
		}
		this.stopped = true;
		// reset counter
		counter.set(0);
	}

	@Override
	public String toString() {
		String outputDir = "";
		if (received.outputDir != null && !stopped) {
			outputDir = String.format(
					", outputDir: \"%s\"",
					this.exportFolder.getName() + 
					File.separator + 
					received.outputDir.getParentFile().getName()
					);
		}
		return String.format(
				"{ " +
				"running: %s, " +
				"obj: %s, " +
				"csv: %s, " +
				"receivedQueue: %d, " +
				"sentQueue: %d, " +
				"written: %d" + 
				outputDir + 
				" }", 
				!stopped, 
				enableObj, 
				enableCsv,
				received.queue.size(), 
				sent.queue.size(), 
				counter.get()
				);
	}

	public boolean isEnableCsv() {
		return enableCsv;
	}

	public void setEnableCsv(boolean enableCsv) {
		this.enableCsv = enableCsv;
	}

	public boolean isEnableObj() {
		return enableObj;
	}

	public void setEnableObj(boolean enableObj) {
		this.enableObj = enableObj;
	}

}
