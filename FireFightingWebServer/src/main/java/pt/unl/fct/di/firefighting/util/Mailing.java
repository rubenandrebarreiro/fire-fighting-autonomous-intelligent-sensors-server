package pt.unl.fct.di.firefighting.util;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailService.Message;
import com.google.appengine.api.mail.MailServiceFactory;

public class Mailing {
	
	//private static final String EMAIL_SMTP = "aspmx.l.google.com";
	private static final String EMAIL = "bd.albergaria@campus.fct.unl.pt";
	private static final String EMAIL_PASSWORD = "scmu2019";
	//private Properties props;
	//private Session session;
	private MailService m = MailServiceFactory.getMailService();
	
	public Mailing(){
		
		
		//this.props = new Properties();
		//this.props.put("mail.smtp.host", EMAIL_SMTP);
		//this.props.put("mail.smtp.socketFactory.port", "25");
		//this.props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		//this.props.put("mail.smtp.auth", "true");
		//this.props.put("mail.smtp.port", "25");
		
		//this.session = Session.getDefaultInstance(props, null);
	}
	
	public void sendMessage(String email, String msg){
		try {
			
			Message message = new Message();
			message.setSender(EMAIL);
			message.setTo(email);
			message.setSubject("FireFighting | Sensor alert");
			message.setTextBody(msg);
		
			//Transport transport = session.getTransport("smtp");
			//transport.connect(EMAIL_SMTP, EMAIL, EMAIL_PASSWORD);
			m.send(message);
			
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
