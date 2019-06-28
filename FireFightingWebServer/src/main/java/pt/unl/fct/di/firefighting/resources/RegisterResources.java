package pt.unl.fct.di.firefighting.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
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

import pt.unl.fct.di.firefighting.util.SensorData;
import pt.unl.fct.di.firefighting.util.UserData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResources {
	
	private static final Logger LOG = Logger.getLogger(RegisterResources.class.getName());
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public RegisterResources() {
		
	}
	
	/**
	 * 
	 * This method recieves a correctly formated JSON and attempts to insert it in
	 * the Google Datastore as a User entity.
	 * 
	 * @param acc
	 * @return success or fail http response code
	 */
	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerUsr(UserData acc) {
		Transaction txn = datastore.beginTransaction();
		
		Query usrQuery = new Query("User");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		
		if(qRes.isEmpty()) {
			LOG.fine("No user accounts in database.");
		} 
		try {
			Key userKey = KeyFactory.createKey("User", acc.email);
			Entity user = datastore.get(userKey);	
			
			txn.rollback();
			return Response.status(Status.BAD_REQUEST).entity("User already exists. ").build();
		} catch(EntityNotFoundException e){
			Entity user = new Entity("User", acc.email);
			user.setProperty("user_mail", acc.email);
			user.setProperty("user_pwd", acc.pass);
			user.setProperty("user_name", acc.name);
			user.setProperty("user_type", acc.usrType);
			user.setProperty("user_creation_time", new Date());			
			datastore.put(txn, user);
			LOG.info("User registered " + acc.email);
			txn.commit();
			return Response.ok().build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
		
	}
	
	/**
	 * 
	 * This method recieves an id via plain text and inserts it in the datastore.
	 * 
	 * @param sensorid
	 * @return success or fail http response code
	 */
	
	// format -> fire-fighting-sensor-N
	@POST
	@Path("/sensor")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response registerSensor(String sensorid) {
		SensorData sensor = new SensorData(sensorid);
		Transaction txn = datastore.beginTransaction();
		Query usrQuery = new Query("Sensor");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No sensor accounts in database.");
		}
		try {
			Key sensorKey = KeyFactory.createKey("Sensor", sensor.sensorId);
			Entity s = datastore.get(sensorKey);	
			
			txn.rollback();
			return Response.status(Status.BAD_REQUEST).entity("Sensor already exists. ").build();
		} catch(EntityNotFoundException e){
			if(sensor.sensorId.equals("fire-fighting-sensor-00")) {
				simulateRegNetwork("fire-fighting-sensor-01");
				simulateRegNetwork("fire-fighting-sensor-02");
			}
			Entity s = new Entity("Sensor", sensor.sensorId);
			s.setProperty("temperature", null);
			s.setProperty("humidity", null);
			s.setProperty("latlon", null);
			s.setProperty("sensor_reg_time", new Date());
			s.setProperty("date_modified", null);
			datastore.put(txn, s);
			LOG.info("sensor registered " + sensor.sensorId);
			txn.commit();
			return Response.ok().build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	/**
	 * 
	 * This is a private method used in this prototype to register two
	 * additional sensor nodes when the real sensor node attempts to register
	 * to simulate a sensor network
	 * 
	 * @param sensor id
	 */
	//hardcoded method to simulate a sensor network by adding 2 others, mirroring readings of the original
	private void simulateRegNetwork(String id) {
		SensorData sensor = new SensorData(id);
		Transaction txn = datastore.beginTransaction();
		Query usrQuery = new Query("Sensor");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No sensor accounts in database.");
		}
		try {
			Key sensorKey = KeyFactory.createKey("Sensor", sensor.sensorId);
			Entity s = datastore.get(sensorKey);
			txn.rollback();
		} catch(EntityNotFoundException e){
			Entity s = new Entity("Sensor", sensor.sensorId);
			s.setProperty("temperature", null);
			s.setProperty("humidity", null);
			s.setProperty("latlon", null);
			s.setProperty("sensor_reg_time", new Date());			
			s.setProperty("date_modified", null);
			datastore.put(txn, s);
			LOG.info("sensor registered " + sensor.sensorId);
			txn.commit();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
}