package selleck.email.pojo;

public class Email {
	private String email;
	private String realURL; // 抓取到email的url
	private String sourceURL; // 来源实验室、机构、BOSS的url
	
	public Email(){
		
	}
	
	public Email(String email,String realURL,String sourceURL){
		this.email = email;
		this.realURL = realURL;
		this.sourceURL = sourceURL;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + this.email.hashCode();
		result = PRIME * result + this.sourceURL.hashCode();
		return result;

	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Email){
			if(((Email) obj).getSourceURL().equals(this.sourceURL) && ((Email) obj).getEmail().equals(this.email)){
				return true;
			}
		}
		return false;
	}
	
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getRealURL() {
		return realURL;
	}
	public void setRealURL(String realURL) {
		this.realURL = realURL;
	}
	public String getSourceURL() {
		return sourceURL;
	}
	public void setSourceURL(String sourceURL) {
		this.sourceURL = sourceURL;
	}
	
	
}
