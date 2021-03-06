package de.fraunhofer.iosb.ilt.frostBenchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.jackson.ObjectMapperFactory;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.LoggerFactory;

public class ProcessorWorker extends MqttHelper implements Runnable {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProcessorWorker.class);

	private String dataStreamTopic = null;

	public void setDataStreamTopic(String dataStreamTopic) {
		this.dataStreamTopic = dataStreamTopic;
	}

	static private long notificationsReceived = 0;

	public ProcessorWorker(String brokerUrl, String clientId, boolean cleanSession) throws MqttException {
		super(brokerUrl, clientId, cleanSession);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			subscribeAndWait(dataStreamTopic, StreamProcessor.qos);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			LOGGER.error(e.toString());
			System.exit(1);
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

		final ObjectMapper mapper = ObjectMapperFactory.get();
		Observation entity;
		try {
			entity = mapper.readValue(message.getPayload(), Observation.class);
			processObservation(entity);
		} catch (IOException e) {
			LOGGER.error("Failed to read message", e);
		}
	}

	private void processObservation(Observation obs) {
		incNotificationsReceived();
		// so something with the observation
		double d = Double.parseDouble(obs.getResult().toString());
		d = d * d;
	}

	public static synchronized long getNotificationsReceived() {
		return notificationsReceived;
	}

	public static synchronized void setNotificationsReceived(long notificationsReceived) {
		ProcessorWorker.notificationsReceived = notificationsReceived;
	}

	public static synchronized void incNotificationsReceived() {
		notificationsReceived++;
	}

}
