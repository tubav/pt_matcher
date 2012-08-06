package de.fhg.fokus.net.packetmatcher.ctrl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.packetmatcher.Matcher;
import de.fhg.fokus.net.packetmatcher.ipfix.PtSync;

/**
 * Handles synchronization of configuration parameters of connected probes.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public final class Console {
	// -- constants --
	private static final String WELCOME_MSG = "===== PT Matcher "+Matcher.VERSION+" ===== \n";
	private static final String PROMPT = "sync> ";
	private static final String CMD_OK = "ok";
	private static final String CMD_SENT = "cmd_sent";
	private static final String CMD_FAILED = "failed";
	private static final String BYE_MSG = "bye";
	private static final String HELP = 
		"h,?                       this help\n" + 
		"c                         clear terminal \n" +
		"f                         show file exporter status \n" +
		"f 0|1                     file exporter on(1) or off(0) \n"+
		"m                         matcher statistics \n" +
		"l,p,p*                    list connected probes\n" +
		"p<nr>                     shows statistics for probe nr \n" +
		"p<nr> x                   close probe connection  \n" +
		"     -r  <sampling rate>  set sampling rate\n"+
		"x                         exit console\n" ;

	// -- sys --
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private static final AtomicLong messageIdCounter = new AtomicLong(1);
	// -- model --
	private final Matcher matcher;
	private final int port;
	private final ServerSocket serverSocket;

	private boolean exit = false;
	private boolean verbose = false;

	private final List<Probe> probeList = new CopyOnWriteArrayList<Probe>();
	public List<Probe> getProbeList() {
		return probeList;
	}

	private final List<Connection> connList = new CopyOnWriteArrayList<Console.Connection>();
	private final Map<Long, Connection> requestMap = new ConcurrentHashMap<Long, Console.Connection>();

	// models a command received via console
	private interface CmdHandler {
		// note that arg[0] will contain the command itself, so you'll
		// need to look for arg[1] for the first argument
 		String execute(Connection conn, String[] args) throws IOException;
	}
	private final Map<String, CmdHandler> cmdMap = new HashMap<String, Console.CmdHandler>();

	private String executeCommand(Connection conn, String cmd) throws IOException {
		if( cmd.length()==0 ){
			return "";
		}
		//		logger.debug("cmd: {}",HexDump.toHexString(cmd.getBytes()));
		// map ^L to clear
		if(cmd.length()==1 && cmd.getBytes("ascii")[0]==0x0c){
			cmd = "c";
		}
		String args[] = cmd.split("[\\s]+");
		if(args!=null&& args.length > 0){
			CmdHandler cmdHandler = cmdMap.get(args[0]);
			if( cmdHandler!=null){
				return  cmdHandler.execute(conn, args);
			} 
		}
		if(cmd.startsWith("p")){
			return "probe not found: "+cmd;
		}

		return CMD_FAILED;
	}
	/**
	 * Handles console connection
	 * 
	 */
	private final class Connection {
		private final Socket socket;
		private final String remoteName; // used for reporting
		private BufferedWriter out;
		private BufferedReader in;

		public Connection(Socket socket) {
			this.socket = socket;
			this.remoteName = socket.getRemoteSocketAddress().toString();
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						in = new BufferedReader(
								new InputStreamReader(Connection.this.socket
										.getInputStream(), "ascii"));
						out = new BufferedWriter(
								new OutputStreamWriter(Connection.this.socket
										.getOutputStream(), "ascii"));
						out.write(WELCOME_MSG);
						out.write("# use h for help\n");
						while (true) {
							out.write(PROMPT);
							out.flush();
							String input = in.readLine();
							if (input == null) {
								break;
							}
							String res = executeCommand(Connection.this, input);
							if( res.length()==0){
								continue;
							}
							out.write(res + "\n");
							out.flush();
						}
						close();
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			});
			
		}
		
		public Socket getSocket() {
			return this.socket;
		}
		
		public void close() throws IOException {
			logger.debug("Closing {}", remoteName);
			socket.close();
		}
	}
	
	public Console(Matcher matcher, int port) throws IOException {
		this.matcher = matcher;
		this.port = port;
		this.serverSocket = new ServerSocket(this.port);
		setupCommands();
	}
	
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private void setupCommands(){
		// == SETUP COMMANDS ==
		// --------------------------------------------------------------------
		// CONSOLE: clear (depends on the terminal used)
		// --------------------------------------------------------------------
		cmdMap.put("c", new CmdHandler() {
			@Override
			public String execute(Connection conn, String [] args) throws IOException {
				conn.out.write("\033[H\033[2J");
				return "";
			}
		});
		// --------------------------------------------------------------------
		// FILE EXPORTER: show stats, turn on/off
		// --------------------------------------------------------------------
		cmdMap.put ("f", new CmdHandler(){
			@Override
			public String execute(Connection conn, String[] args)
			throws IOException {
				if(args.length > 1 ){
					if("1".contentEquals(args[1])&& matcher.getFileExporter().isStopped() ){
						matcher.getFileExporter().setEnableCsv(true);
						matcher.getFileExporter().setEnableObj(true);
						matcher.getFileExporter().start();
					}
					if("0".contentEquals(args[1])&& !matcher.getFileExporter().isStopped() ){
						matcher.getFileExporter().stop();
					}
				}
				conn.out.write(matcher.getFileExporter().toString());
				conn.out.write("\n");

				return "";
			}
		});
		// --------------------------------------------------------------------
		// MATCHER: show statistics
		// --------------------------------------------------------------------
		cmdMap.put ("m", new CmdHandler(){
			@Override
			public String execute(Connection conn, String[] args)
			throws IOException {
				String matcherStr = matcher.toString();
				conn.out.write(matcher.toString());
				conn.out.write("\n");
				logger.debug(matcherStr.replaceAll("\\s+|\n", " "));
				return "";
			}
		});
		// --------------------------------------------------------------------
		// PROBES: list connected
		// --------------------------------------------------------------------
		CmdHandler listProbes = new CmdHandler() {
			@Override
			public String execute(Connection conn, String [] args) throws IOException {
				if(probeList.size()==0){
					return ("no probe connected");
				}
				int i=0;
				StringBuffer sbuf = new StringBuffer();
				for(Probe probe: probeList ){
					sbuf.append(++i+". ");
					sbuf.append(probe.toString());
					sbuf.append("\n");
				}
				logger.debug("PROBE_LIST: {} ",sbuf.toString().trim());
				conn.out.write(sbuf.toString());
				return "";
			}
		};
		cmdMap.put("p",listProbes);
		cmdMap.put("l",listProbes);
		// --------------------------------------------------------------------
		// ALL PROBES: send command
		// --------------------------------------------------------------------
		cmdMap.put( "p*", new CmdHandler(){
			@Override
			public String execute(Connection conn, String[] args)
			throws IOException {
				if(probeList.size()==0){
					return ("no probe connected");
				}
				int i=0;
				if( args.length==1){
					StringBuffer sbuf = new StringBuffer();
					for(Probe probe: probeList ){
						sbuf.append(++i+". ");
						sbuf.append(probe.toString());
						sbuf.append("\n");
					}
					logger.debug("PROBE_LIST: {}",sbuf.toString().trim());
					conn.out.write(sbuf.toString());
					return "";
				}
				for(Probe probe: probeList ){
					String cmd = prepareCmdToSend(conn, args);
					conn.out.write(String.format("send( %s , \"%s\" )\n",probe.getConsoleId(), cmd));
					probe.sendCmd(cmd);
				}
				return "";
			}
		});
		// --------------------------------------------------------------------
		// HELP: show
		// --------------------------------------------------------------------
		CmdHandler help =  new CmdHandler() {
			@Override
			public String execute(Connection conn, String [] args) throws IOException {
				conn.out.write(WELCOME_MSG);
				conn.out.write(HELP);
				return "";
			}
		};
		cmdMap.put("h",help);
		cmdMap.put("?",help);

		// --------------------------------------------------------------------
		// CONSOLE: exit
		// --------------------------------------------------------------------
		cmdMap.put("x", new CmdHandler() {
			@Override
			public String execute(Connection conn, String [] args) {
				try {
					conn.out.write(BYE_MSG+"\n");
					conn.out.flush();
					conn.close();
				} catch (IOException e) {
					// ignoring
				}
				return CMD_OK;
			}
		});
	}

	public void start() {
		executor.execute(new Runnable() {
			public void run() {
				logger.debug("starting probe sync: {}", serverSocket);
				while (!exit) {
					try {
						Connection newConnection = new Connection(serverSocket.accept());
						logger.info("adding new connection: {}", newConnection.getSocket());
						connList.add(newConnection);
					} catch (IOException e) {
						logger.error(e.getMessage());

					}
				}
			}
		});
		
		// Debug timer
		if( isVerbose() ){
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					for( Probe probe: getProbeList() ){
						if(probe!=null){
							logger.debug("PROBE_DETAILED_STATS: {}",probe.getStats().replaceAll("\\s+|\n", " "));
							logger.debug("MATCHER_STATS: {}",toString().replaceAll("\\s+|\n", " ") );
						}
					}
				}
			} , 0, 10, TimeUnit.SECONDS);
		}
	}
	
	public void receive( Probe probe, PtSync syncResp ){
		if(syncResp==null){
			logger.error("strange, sync response is null.");
			return;
		}
		logger.debug("PROBE_RESPONSE: probe: {}, response: {}",probe.getConsoleId(), syncResp);
		Connection conn = requestMap.remove(syncResp.getMessageId());
		if(conn!=null && conn.socket.isConnected() && probe!=null ){
			try {
				conn.out.write("\n# sync response: ");
				conn.out.write(probe.getConsoleId()+" ");
				conn.out.write(syncResp+"");
				conn.out.flush();
			} catch (IOException e) {
				logger.error(e.getMessage());
			} 
		} else {
			logger.error("error while receiving probe response");
		}
	}
	public void addProbe( final Probe probe ){
		// adding custom command for probe
		cmdMap.put(probe.getConsoleId(), new CmdHandler() {
			@Override
			public String execute(Connection conn, String[] args) throws IOException {
				if(args.length==0){
					logger.warn("Strange, this should never happen. Probably a bug.");
					return CMD_FAILED;
				}
				if(args.length==1){
					conn.out.write(probe.getStats());
					conn.out.write("\n");
					logger.debug(probe.getStats().replaceAll("\\s+|\n", " "));
					return "";
				} 
				if(args[1].contentEquals("x")){
					logger.debug("console requested closing {}",conn.socket.getRemoteSocketAddress());
					conn.out.write("dropping "+probe.toString()+"\n");
					removeProbe(probe);
					probe.getIpfixConnectionHandler().getSocket().close();
					return "";
				} else {
					// send command directly to probe
					if( probe.sendCmd(prepareCmdToSend(conn, args))){
						return CMD_SENT;
					} 
				}
				return CMD_FAILED;
			}
		});
		logger.info("add probe {}", probe.toString());
		probeList.add(probe);

	}
	public void removeProbe( Probe probe ){
		cmdMap.remove(probe.getConsoleId());
		probeList.remove(probe);
	}
	/**
	 * Prepare command to send to probe
	 * @param args
	 * @return
	 */
	public  String prepareCmdToSend( Connection conn, String []args ){
		long messageId = messageIdCounter.getAndIncrement();
		requestMap.put(messageId, conn); // storing request so we can forward response to the right connection
		StringBuilder sb = new StringBuilder();
		sb.append("mid: ");
		sb.append(messageId);
		sb.append(" ");
		for( int i=1;i<args.length;i++){
			sb.append(args[i]);
			sb.append(" ");
		}
		return sb.toString();
	}
	public void shutdown() {
		logger.info("shutting down console sync");
		exit = true;
		try {
			for (Connection conn : connList) {
				conn.close();
			}
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
		executor.shutdown();
	}

}
