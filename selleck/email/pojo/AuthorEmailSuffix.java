package selleck.email.pojo;

public class AuthorEmailSuffix {
	private int id;
	private String title;
	private String author;
	private String address;
	private String organization;
	private String emailSuffix;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public String getEmailSuffix() {
		return emailSuffix;
	}
	public void setEmailSuffix(String emailSuffix) {
		this.emailSuffix = emailSuffix;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	
}
