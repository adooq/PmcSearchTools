package selleck.email.wosaccount;

import java.util.Map;

public abstract class IAccount {
	String loginUrl = "";
	String userName = "";
	String password = "";
	String searchUrl = "";
	
	public abstract Map<String,String> login();

	public String getLoginUrl() {
		return loginUrl;
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSearchUrl() {
		return searchUrl;
	}

	public void setSearchUrl(String searchUrl) {
		this.searchUrl = searchUrl;
	}
	
	
}
