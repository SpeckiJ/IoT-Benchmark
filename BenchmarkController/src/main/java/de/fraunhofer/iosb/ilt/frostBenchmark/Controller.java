package de.fraunhofer.iosb.ilt.frostBenchmark;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.dao.BaseDao;
import de.fraunhofer.iosb.ilt.sta.model.Entity;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
import org.slf4j.LoggerFactory;

public class Controller {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Controller.class);

	static final String BENCHMARK = "Benchmark";
	static final String SESSION = "session";

	public static void main(String[] args)
			throws IOException, URISyntaxException, ServiceFailureException, InterruptedException {
		String cmdInfo = "Available command are <run [msec]>, <stop>, <script file>, <terminate>, <help>, <delete>, <quit>";

		BenchData.initialize();
		Scheduler scriptScheduler = new Scheduler();

		Thing myThing = BenchData.getBenchmarkThing();

		System.out.println(cmdInfo);
		boolean running = true;
		Scanner sc = new Scanner(System.in);
		while (running) {
			System.out.print("Benchmark > ");
			String[] cmd = sc.nextLine().split(" ");
			if (cmd[0].equalsIgnoreCase("run")) {
				// processing the RUN command -------------------------------
				scriptScheduler.sendCommands(null, BenchProperties.STATUS.RUNNING);
				if (cmd.length > 1) {
					int ms = Integer.parseInt(cmd[1]);
					System.out.println("running for " + ms + " msec");
					Thread.sleep(ms);
					scriptScheduler.sendCommands(null, BenchProperties.STATUS.FINISHED);
				}
			} else if (cmd[0].equalsIgnoreCase("stop")) {
				// processing the STOP command ----------------------------
				scriptScheduler.sendCommands(null, BenchProperties.STATUS.FINISHED);
			} else if (cmd[0].equalsIgnoreCase("delete")) {
				// processing the DELETE command --------------------------
				System.out.println("All data in " + BenchData.baseUri.toString()
						+ " will be deleted. After that you need to restart");
				System.out.println("Are you sure you want to do this? Type 'yes'");
				String answer = sc.nextLine();
				if (answer.equalsIgnoreCase("YES")) {
					System.out.println("ok, let's do it");
					deleteAll(BenchData.service);
					System.out.println("finished - you need to restart");
					System.exit(0);
				} else {
					System.out.println("fine, we keep the data");
				}
				System.out.println(cmdInfo);
			} else if (cmd[0].equalsIgnoreCase("script") || cmd[0].equalsIgnoreCase("s")) {
				// processing the SCRIPT command -------------------------------
				if (cmd.length > 1) {
					System.out.println("running script " + cmd[1]);
					scriptScheduler.readSchedule(cmd[1]);
					scriptScheduler.runScript();
				} else {
					System.out.println("missing script name");
				}
			} else if (cmd[0].equalsIgnoreCase("terminate") || cmd[0].equalsIgnoreCase("t")) {
				// processing the TERMINATE command ------------------------
				scriptScheduler.sendCommands(null, BenchProperties.STATUS.TERMINATE);
				System.out.println("Terminate message sent");
			} else if (cmd[0].equalsIgnoreCase("help") || cmd[0].equalsIgnoreCase("h")) {
				// processing the HELP command ---------------------------------------------
				System.out.println("Base URL      : " + BenchData.baseUri.toString());
				System.out.println("Session Id    : " + BenchData.sessionId);
				System.out.println("<run [msec]>  : Start all benchmark process with optional parameter time im msec");
				System.out.println("<script file> : Start all benchmark script with file name");
				System.out.println("<stop>        : Stop all running processes");
				System.out.println("<terminate>   : Terminte all running benchmark processes");
				System.out.println("<delete>      : Deletes all data in base url - THINK TWICE BEFORE USING THIS!!!");
				System.out.println("<help>        : print this help info");
				System.out.println("<quit>        : Quit this Controller terminal");
			} else if (cmd[0].equalsIgnoreCase("quit") || cmd[0].equalsIgnoreCase("q")) {
				running = false;
				System.out.println("Bye");
			}
		}
		sc.close();
	}

	public static void deleteAll(SensorThingsService sts) throws ServiceFailureException {
		deleteAll(sts.things());
		deleteAll(sts.locations());
		deleteAll(sts.sensors());
		deleteAll(sts.featuresOfInterest());
		deleteAll(sts.observedProperties());
		deleteAll(sts.observations());
	}

	public static <T extends Entity<T>> void deleteAll(BaseDao<T> doa) throws ServiceFailureException {
		boolean more = true;
		int count = 0;
		while (more) {
			EntityList<T> entities = doa.query().count().list();
			if (entities.getCount() > 0) {
				BenchData.LOGGER.info("{} to go.", entities.getCount());
			} else {
				more = false;
			}
			for (T entity : entities) {
				doa.delete(entity);
				count++;
			}
		}
		BenchData.LOGGER.info("Deleted {} using {}.", count, doa.getClass().getName());
	}

}