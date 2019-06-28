package pt.unl.fct.di.firefighting.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@Path("/cron")
public class BackgroundResources {
	
	
	private static final long TWELVE_HOURS = 43200000;
	private static final Logger LOG = Logger.getLogger(BackgroundResources.class.getName());
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public BackgroundResources() {
		
	}
	
	
	/**
	 * 
	 * This method, designed to work as a cron job, removes sensors from the datastore
	 * when their last info sent was more than twelve hours, thus assuming the node is 
	 * not operational.
	 * 
	 * @return http response code
	 */
	@POST
	@Path("/clean")
	public Response clearInactiveSensorsTask() {
		LOG.fine("Starting to execute computation taks");
		
		try {
			Query ctrQuery = new Query("Sensor");
			List<Entity> results = datastore.prepare(ctrQuery).asList(FetchOptions.Builder.withDefaults());
			List<Key> keys = new ArrayList<Key>();
			if(!results.isEmpty()){
				for(Entity result:results){
					if(result.getProperty("date_modified") == null) {
						continue; //for testing purposes only: default sensor behaviour only uses null field on registration
					}
					if((System.currentTimeMillis() - ((java.util.Date) result.getProperty("date_modified")).getTime()) < TWELVE_HOURS){
						results.remove(result);
					} else {
						keys.add(result.getKey());
					}
				}
				if(!keys.isEmpty()) {
					datastore.delete(keys);					
				}
			}
		} catch (Exception e) {
			LOG.logp(Level.SEVERE, this.getClass().getCanonicalName(), "clearInactiveSensorsTask", "An exception has ocurred", e);
			return Response.serverError().build();
		} 
		return Response.ok().build();
	}
	
	
	/**
	 * 
	 * This method adds the removal method above to the cron task queue.
	 * 
	 * @return http response code
	 */
	@GET
	@Path("/compute")
	public Response triggerCronJobs() {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(TaskOptions.Builder.withUrl("/cron/clean"));
		return Response.ok().build();
	}
	
}
