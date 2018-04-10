package frostBenchmark;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Thing;

public class SensorCluster extends MqttHelper {

	public SensorCluster(String brokerUrl, String clientId, boolean cleanSession) throws MqttException {
		super(brokerUrl, clientId, cleanSession);
		// TODO Auto-generated constructor stub
	}

	/**
	 * The main entry point of the sample.
	 *
	 * This method handles parsing the arguments specified on the command-line
	 * before performing the specified action.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws ServiceFailureException
	 */
	public static void main(String[] args) throws IOException, URISyntaxException, ServiceFailureException {

		String topic = null;
		int qos = 2;
		String broker;
		int port = 1883;
		String clientId = "BechmarkSensorCluster-" + System.currentTimeMillis();
		boolean cleanSession = true; // Non durable subscriptions
		String protocol = "tcp://";

		BenchData.initialize();
		Thing benchmarkThing = BenchData.getBenchmarkThing();

		Run.initWorkLoad();

		topic = "v1.0/Things(" + benchmarkThing.getId().toString() + ")/properties";

		try {
			// Create an instance of the Sample client wrapper
			broker = System.getenv(Run.BROKER);
			if (broker == null) {
				broker = "localhost";
			}
			Run.LOGGER.trace("using mqtt broker: " + broker);
			String url = protocol + broker + ":" + port;
			SensorCluster sensors = new SensorCluster(url, clientId, cleanSession);

			sensors.subscribe(topic, qos);

		} catch (MqttException me) {
			// Display full details of any exception that occurs
			Run.LOGGER.error("reason " + me.getReasonCode());
			Run.LOGGER.error("msg " + me.getMessage());
			Run.LOGGER.error("loc " + me.getLocalizedMessage());
			Run.LOGGER.error("cause " + me.getCause());
			Run.LOGGER.error("excep " + me);
			me.printStackTrace();
		} catch (Throwable th) {
			Run.LOGGER.error("Throwable caught " + th);
			th.printStackTrace();
		}
	}

	@Override
	/**
	 * @throws URISyntaxException
	 * @throws ServiceFailureException
	 * @see MqttCallback#messageArrived(String, MqttMessage)
	 */
	public void messageArrived(String topic, MqttMessage message)
			throws MqttException, ServiceFailureException, URISyntaxException {

		JSONObject msg = new JSONObject(new String(message.getPayload()));
		JSONObject p = (JSONObject) msg.get("properties");
		String benchState = p.getString("state");

		Run.LOGGER.info("Entering " + benchState + " mode");

		if (benchState.equalsIgnoreCase(RUNNING)) {
			// start the client
			Run.startWorkLoad();
		} else if (benchState.equalsIgnoreCase(FINISHED)) {
			// get the results
			Run.stopWorkLoad();
		} else if (benchState.equalsIgnoreCase(TERMINATE)) {
			Run.LOGGER.info("Terminate Command received - exit process");
			state = DISCONNECT;
			this.waiter.notify();
		}
	}
}
