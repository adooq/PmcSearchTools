package selleck.email.wosaccount;

import java.util.HashMap;
import java.util.Map;

import selleck.utils.HTTPUtils;

public class Uwe extends IAccount {

	@Override
	public Map<String, String> login() {
		Map<String,String> params = new HashMap<String,String>();
		params.put("username", this.getUserName());
		params.put("password", this.getPassword());
		params.put("curl", "Z2FoalaZ2FloginZ2Fuwe-auth");
		params.put("flags", "0");
		params.put("forcedownlevel", "0");
		params.put("formdir", "7");
		params.put("trusted", "0");
		params.put("loginButton", "Login");
		
	
		Map<String,String> loginMap = HTTPUtils.getCookieUrlAndHtml(this.getLoginUrl(), null ,null, HTTPUtils.POST ,params);
// 		String cookies = loginMap.get("cookie");
		String url = loginMap.get("url");
//		System.out.println("final url: " + url);
//		System.out.println("final cookies: " + cookies);
		// System.out.println("final html: " + loginMap.get("html"));
		
		if(loginMap.size() == 0 || !url.contains("GeneralSearch_input.do")){
			return null;
		}

		return loginMap;
	}

}
