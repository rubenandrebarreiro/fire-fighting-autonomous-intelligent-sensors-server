package pt.unl.fct.di.firefighting.util;

public class UserData {
	
	public String email;
	public String pass;
	public String name;
	public String latlon;
	public String usrType;
	
	public UserData(){
		
	}
	
	public UserData(String password, String email, String name, String usrType){
		this.email = email;
		pass = password;
		this.name = name;
		this.usrType = usrType;
		
	}
	
	public void setEmail(String mail) {
		email = mail;
	}
	
	public UserData(String mail, String latlon) {
		email = mail;
		this.latlon = latlon;
	}
	
}
