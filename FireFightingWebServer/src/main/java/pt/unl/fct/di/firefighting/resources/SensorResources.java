package pt.unl.fct.di.firefighting.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

import pt.unl.fct.di.firefighting.util.Mailing;

@Path("/sensor")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class SensorResources {

	
	public SensorResources() {
		
	}
	
	private static final Logger LOG = Logger.getLogger(SensorResources.class.getName());
	private static final Mailing m = new Mailing();
	//private final Gson g = new Gson();
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	
	
	/**
	 * 
	 * This method receives readings from a registered sensor node and stores them in
	 * the Google datastore.
	 * 
	 * @param readings (String) -> expected format "sensorID temperature humidity"
	 * @return http response code
	 */
	// expected format "sensorID temperature humidity" in body
	@PUT
	@Path("/read")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response setReadings(String readings) {
		String[] data = readings.split(" ");
		Transaction txn = datastore.beginTransaction();
		Query usrQuery = new Query("Sensor");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No sensor accounts in database.");
			return Response.status(Status.BAD_REQUEST).build();
		}
		try {
			if(data[0].equals("fire-fighting-sensor-00")) {
				replicateReadings("fire-fighting-sensor-01",data[1],data[2]);
				replicateReadings("fire-fighting-sensor-02",data[1],data[2]);
			}
			Key sensorKey = KeyFactory.createKey("Sensor", data[0]);
			Entity s = datastore.get(sensorKey);	
			s.setProperty("temperature", data[1]);
			s.setProperty("humidity", data[2]);
			s.setProperty("date_modified", new Date());
			datastore.put(txn, s);
			txn.commit();
			return Response.ok().entity("Sensor readings updated. ").build();
		} catch(EntityNotFoundException e){
			LOG.info("Sensor " + data[0] + " not found. ");
			txn.rollback();
			return Response.status(Status.BAD_REQUEST).build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	/**
	 * 
	 * This method receives the sensor id of a node that is detecting fire, registers
	 * the occurrence in the datastore and sends an e-mail to every user notifying them.
	 * 
	 * @param sensor id
	 * @return http response code
	 */
	//Expected "sensorID" in body
	@POST
	@Path("/alert")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response fireAlert(String sensor) {
		Transaction txn = datastore.beginTransaction();
		Key sensorKey = KeyFactory.createKey("Sensor", sensor);
		try {
			Entity node = datastore.get(sensorKey); //Verification that the sensor exists in db
			Entity historyLog = new Entity("FireHistory", node.getKey());
			historyLog.setProperty("sensorID", sensor);
			historyLog.setProperty("date_added", new Date());
			List<Entity> entities = Arrays.asList(node,historyLog);
			datastore.put(txn, entities);
			txn.commit();
			sendAlertMail(sensor);
			return Response.ok().entity("Log recorded").build();
		} catch (EntityNotFoundException e) {
			// TODO: handle exception
			txn.rollback();
			return Response.status(Status.BAD_REQUEST).build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	/**
	 * 
	 * This method, like in the registration phase, replicates the readings of the functional
	 * sensor node to the "dummy" ones created from the first to simulate a sensor node network
	 * 
	 * @param id
	 * @param temp
	 * @param hum
	 */
	//hardcoded method to replicate readings to the dummy sensors
	private void replicateReadings(String id, String temp, String hum){
		Transaction txn = datastore.beginTransaction();
		Query usrQuery = new Query("Sensor");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No sensor accounts in database.");
		}
		try {
			Key sensorKey = KeyFactory.createKey("Sensor", id);
			Entity s = datastore.get(sensorKey);	
			s.setProperty("temperature", temp);
			s.setProperty("humidity", hum);
			s.setProperty("date_modified", new Date());
			datastore.put(txn, s);
			txn.commit();
		} catch(EntityNotFoundException e){
			LOG.info("Sensor " + id + " not found. ");
			txn.rollback();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	/**
	 * 
	 * Private method called in the "fireAlert" method that sends an e-mail notification 
	 * to all registered users.
	 * 
	 * @param sensorID
	 */
	private void sendAlertMail(String sensorID) {
		Query usrQuery = new Query("User");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No users to send alerts. ");
		} else {
			for(Entity e : qRes) {
				String email = (String) e.getProperty("user_mail");
				m.sendMessage(email, "Sensor " + sensorID + " has detected fire.");
			}
		}
	}
}
