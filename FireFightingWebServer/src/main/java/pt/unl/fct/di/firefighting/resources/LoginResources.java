package pt.unl.fct.di.firefighting.resources;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.firefighting.util.SensorData;
import pt.unl.fct.di.firefighting.util.UserData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResources {

	private static final Logger LOG = Logger.getLogger(LoginResources.class.getName());
	private final Gson g = new Gson();
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();


	public LoginResources(){

	}

	/**
	 * 
	 * This method defines a user's log in storing assorted information like
	 * location and time for logs and georeferencing.
	 * 
	 * @param acc
	 * @param request
	 * @param headers
	 * @return http response code
	 */
	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLoggin(UserData acc,@Context HttpServletRequest request,@Context HttpHeaders headers){
		if (acc.email == null) {
			LOG.fine("no accounts in database.");
			return Response.status(Status.FORBIDDEN).build();
		}
	
		LOG.fine("Atempting to log in: " + acc.email);
	
		Transaction txn = datastore.beginTransaction();
		Key userKey = KeyFactory.createKey("User", acc.email);
		try {
			Entity user = datastore.get(userKey);
			user.setProperty("latlon", headers.getHeaderString("X-AppEngine-CityLatLong"));
			Query ctrQuery = new Query("UserStats").setAncestor(userKey);
			List<Entity> results = datastore.prepare(ctrQuery).asList(FetchOptions.Builder.withDefaults());
			Entity uStats = null;
			if(results.isEmpty()) {
				uStats = new Entity("userStats", user.getKey());
				uStats.setProperty("user_login_attempts", 0L);
				uStats.setProperty("user_login_failed", 0L);
			} else {
				uStats = results.get(0);
			}
			String pwd = (String) user.getProperty("user_pwd");
			if (pwd.equals(acc.pass)) {
				Entity log = new Entity("UserLog", user.getKey());
	
				log.setProperty("user_login_ip", request.getRemoteAddr());
				log.setProperty("user_login_host", request.getRemoteHost());
				log.setProperty("user_login_latlon", headers.getHeaderString("X-AppEngine-CityLatLong"));
				log.setProperty("user_login_city", headers.getHeaderString("X-AppEngine-City"));
				log.setProperty("user_login_country", headers.getHeaderString("X-AppEngine-Country"));
				log.setProperty("user_login_time", new Date());
	
				uStats.setProperty("user_login_attempts", 1L + (long) uStats.getProperty("user_login_attempts"));
				uStats.setProperty("user_login_failed", 0L);
				uStats.setProperty("user_last_login", new Date());
	
				List<Entity> logs = Arrays.asList(log, uStats, user);
				datastore.put(txn, logs);
				txn.commit();
	
				LOG.info("User '" + acc.email + "' logged in successfully.");
				return Response.ok().build();
			} else {
				uStats.setProperty("user_login_failed", 1L + (long) uStats.getProperty("user_login_failed"));
				LOG.warning("Incorrect password for username: " + acc.email);
				datastore.put(txn, uStats);
				txn.commit();
				return Response.status(Status.FORBIDDEN).build();
			}
		} catch(EntityNotFoundException e) {
			LOG.warning("Failed login attempt for username: " + acc.email);
			return Response.status(Status.FORBIDDEN).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

}

	/**
	 * 
	 * This method sets a location to a sensor that hasn't been attributed a location
	 * 
	 * @param sensor
	 * @return http response code
	 */
	@PUT
	@Path("/regSensorLoc")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerSensorLocation(SensorData sensor) {
		Transaction txn = datastore.beginTransaction();
		Query usrQuery = new Query("Sensor");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No sensor accounts in database.");
		}
		try {
			Key sensorKey = KeyFactory.createKey("Sensor", sensor.sensorId);
			Entity s = datastore.get(sensorKey);	
			s.setProperty("latlon", sensor.latlon);
			datastore.put(txn, s);
			txn.commit();
			return Response.ok().entity("Sensor location updated. ").build();
		} catch(EntityNotFoundException e){
			LOG.info("sensor " + sensor.sensorId + " not found. ");
			txn.rollback();
			return Response.status(Status.BAD_REQUEST).build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	/**
	 * 
	 * This method removes a registered user from the datastore
	 * 
	 * @param user
	 * @return http response code
	 */
	@DELETE
	@Path("/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response removeUser(UserData user) {
		Key userKey = KeyFactory.createKey("User", user.email);
		Query ctrQuery = new Query("User").setAncestor(userKey);
		List<Entity> results = datastore.prepare(ctrQuery).asList(FetchOptions.Builder.withDefaults());
		if(results.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("User not found. ").build();
		}
		datastore.delete(userKey);
		return Response.ok().entity("User removed. ").build();
	}
	
	
	/**
	 * 
	 * This method returns a JSON object containing registered sensors without a set location.
	 * 
	 * @return JSON object
	 */
	@GET
	@Path("/getSensorNoLoc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNoLocSensors() {
		Filter propertyFilter = new FilterPredicate("latlon", FilterOperator.EQUAL, null);
		Query usrQuery = new Query("Sensor").setFilter(propertyFilter);
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			return Response.ok().entity("No unregistered sensors in database. ").build();
		}
		return Response.ok().entity(g.toJson(qRes)).build();
		
	}
	
	/**
	 * 
	 * This method returns a JSON object containing registered sensors with a set location.
	 * 
	 * @return JSON object
	 */
	@GET
	@Path("/getSensorLocs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLocSensors() {
		Filter propertyFilter = new FilterPredicate("latlon", FilterOperator.NOT_EQUAL, null);
		Query usrQuery = new Query("Sensor").setFilter(propertyFilter);
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			return Response.ok().entity("No registered sensors in database. ").build();
		}
		return Response.ok().entity(g.toJson(qRes)).build();
		
	}
	
	/**
	 * 
	 * This method updates the location of a user, changing his entry on the datastore
	 * 
	 * @param usr
	 * @return http response code
	 */
	@POST
	@Path("/updateLoc")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUserLoc(UserData usr) {
		Transaction txn = datastore.beginTransaction();
		Query usrQuery = new Query("User");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			LOG.fine("No user accounts in database.");
		} 
		try {
			Key userKey = KeyFactory.createKey("User", usr.email);
			Entity user = datastore.get(userKey);	
			user.setProperty("latlon", usr.latlon);
			
			datastore.put(txn, user);
			txn.commit();
			return Response.ok().build();			
		} catch(EntityNotFoundException e){
			txn.rollback();
			return Response.status(Status.NOT_FOUND).entity("User not found. ").build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
		
	}
	
	
	/**
	 * 
	 * This method returns a JSON object containing registered user's location.
	 * 
	 * @return JSON object
	 */
	@GET
	@Path("/getUserLocs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLocUsers() {
		Filter propertyFilter = new FilterPredicate("latlon", FilterOperator.NOT_EQUAL, null);
		Query usrQuery = new Query("User").setFilter(propertyFilter);
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			return Response.ok().entity("No registered sensors in database. ").build();
		}
		return Response.ok().entity(g.toJson(qRes)).build();
		
	}
	
	/**
	 * 
	 * This method returns a JSON object containing the registered sensors' log of fire detection.
	 * 
	 * @return JSON object
	 */
	@GET
	@Path("/getSensorAlerts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSensorAlerts() {
		Query usrQuery = new Query("FireHistory");
		List<Entity> qRes = datastore.prepare(usrQuery).asList(FetchOptions.Builder.withDefaults());
		if(qRes.isEmpty()) {
			return Response.ok().entity("No registered fires in database. ").build();
		}
		return Response.ok().entity(g.toJson(qRes)).build();
		
	}
	
	

}
