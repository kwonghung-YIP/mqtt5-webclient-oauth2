package org.hung.config;

import java.nio.charset.StandardCharsets;

import javax.annotation.PreDestroy;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MqttBrokerConfig {

	@Value("${mqtt.client-id}")
	private String clientId;
	
	@Value("${mqtt.broker-url}")
	private String brokerUrl;

	@Value("${mqtt.username:}")
	private String username;
	
	@Value("${mqtt.password:}")
	private String password;
	
	private MqttAsyncClient client;
	
	@Bean
	public MqttAsyncClient mqttClient() throws MqttException {

		MqttClientPersistence persistence = new MemoryPersistence();
		client = new MqttAsyncClient(brokerUrl, clientId, persistence);
		
		MqttConnectionOptions options = new MqttConnectionOptions();
		if (StringUtils.hasText(username) || StringUtils.hasText(password)) {
			options.setUserName(username);
			options.setPassword(password.getBytes(StandardCharsets.UTF_8));
		}
		
		client.setCallback(new MqttCallback() {
			
			@Override
			public void mqttErrorOccurred(MqttException exception) {
				log.error("MQTT Client error occurred",exception);
			}
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void disconnected(MqttDisconnectResponse disconnectResponse) {
				log.info("MQTT Client disconnected");	
			}
			
			@Override
			public void deliveryComplete(IMqttToken token) {
				try {
					log.info("MQTT Client delivery completed. {} {}",new String(token.getMessage().getPayload(),StandardCharsets.UTF_8), token.getTopics());
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				log.info("MQTT Client connect completed");
			}
			
			@Override
			public void authPacketArrived(int reasonCode, MqttProperties properties) {
				// TODO Auto-generated method stub
				
			}
		});
		
		IMqttToken token = client.connect(options);
		token.waitForCompletion();
		
		return client;
	}
	
	@PreDestroy
	public void killMqttClient() {
		try {
			log.info("Closing MQTT client before shutdown the springboot application...");
			client.disconnect();
			client.close();
			log.info("MQTT client closed.");
		} catch (MqttException e) {
			log.error("error raised when closing the MQTT client",e);
		}
	}
}
