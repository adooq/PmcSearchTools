package selleck.email.wosaccount;

import java.util.HashMap;
import java.util.Map;

import selleck.utils.HTTPUtils;


/**
 * 使用bangor账号的登陆
 * @author fscai
 *
 */
public class Bangor extends IAccount {

	@Override
	public Map<String,String> login() {
		Map<String,String> params = new HashMap<String,String>();
		// params.put("url", "http://0-apps.webofknowledge.com.unicat.bangor.ac.uk:80/");
		params.put("extpatid", this.getUserName());
		params.put("extpatpw", this.getPassword());
		params.put("submit.x", "34");
		params.put("submit.y", "21");
		params.put("code", "");
		params.put("pin", "");
	
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
