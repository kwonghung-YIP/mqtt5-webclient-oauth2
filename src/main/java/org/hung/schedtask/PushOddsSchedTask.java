package org.hung.schedtask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.hung.apiwrapper.InfoApiWrapper;
import org.hung.pojo.OddsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class PushOddsSchedTask {

	@Autowired
	private InfoApiWrapper info;
	
	@Autowired
	private MqttAsyncClient mqtt;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private Duration lastWinPushInterval;
	private JsonNode lastWinOdds;
	
	
	@Scheduled(fixedRateString="1000")
	public void pushCurrentRaceWinOdds() {
		String meetingdate = "2021-11-03";
		String venue = "HV";
		String raceno = "1";

		//Duration winTranFreq = Duration.ofMinutes(5);
		Duration winTranFreq = Duration.ofSeconds(6);
		
		if (lastWinPushInterval != null && lastWinPushInterval.compareTo(winTranFreq)<0) {
			log.info("{} ms to next WIN odds refresh cycle",(winTranFreq.toMillis()-lastWinPushInterval.toMillis()));
			lastWinPushInterval = lastWinPushInterval.plusMillis(1000);
		} else {
			log.info("fetch WIN Odds and push to Mqtt");
			//info.pmpoolsodds(meetingdate, venue, raceno, "WIN");
			//mono.subscribe(log::info);
			Mono<JsonNode> mono = info.pmpoolsodds(meetingdate, venue, raceno, "WIN");
			mono.subscribe(json -> {
				try {
					JsonNode node = json.get("ra").get(0).get("pl").get(0).get("oddsInfo");
					OddsInfo oddsInfo = objectMapper.treeToValue(node,OddsInfo.class);
					log.info("Read json: {}",oddsInfo);
					
					if (lastWinOdds!=null && lastWinOdds.equals(node)) {
						log.info("Last Win Odds has no change");
					} else {
						String topic = String.format("public/push/odds/%s/%s/%s/win", meetingdate, venue, raceno);
						//pushRawPojo(topic, oddsInfo);
						pushGZipPojo(topic, oddsInfo);
						
						log.info("reset lastPushInterval to zero");
						lastWinPushInterval = Duration.ZERO;
						lastWinOdds = node;
					}
				} catch (JsonProcessingException | IllegalArgumentException e) {
					log.error("",e);
				}
			});

		}
	}
	
	private void pushRawPojo(String topic,Object pojo) {
		MqttProperties props = new MqttProperties();
		props.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
		props.setMessageExpiryInterval(60*15l);
			
		MqttMessage msg = new MqttMessage();
		msg.setRetained(true);
		msg.setQos(1);
		msg.setProperties(props);
		
		try {
			msg.setPayload(objectMapper.writeValueAsBytes(pojo));
		} catch (JsonProcessingException e) {
			log.error("", e);
		}
		
		try {
			IMqttToken token = mqtt.publish(topic, msg);
			token.waitForCompletion();
		} catch (MqttException e) {
			log.error("fail to publish message",e);
		}			
	}
	
	private void pushGZipPojo(String topic,Object pojo) {
		
		MqttProperties props = new MqttProperties();
		props.setContentType("application/gzip");
		props.setMessageExpiryInterval(60*15l);
			
		MqttMessage msg = new MqttMessage();
		msg.setRetained(true);
		msg.setQos(1);
		msg.setProperties(props);
		
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
			GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut);
			objectMapper.writeValue(gzipOut,pojo);
			msg.setPayload(byteOut.toByteArray());
		} catch (IOException e) {
			log.error("", e);
		}
		
		try {
			IMqttToken token = mqtt.publish(topic, msg);
			token.waitForCompletion();
		} catch (MqttException e) {
			log.error("fail to publish message",e);
		}			
	}
}
