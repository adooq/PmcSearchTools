package selleck.email.wosaccount;

import java.util.HashMap;
import java.util.Map;

import selleck.utils.HTTPUtils;


/**
 * 通用的账号登陆
 * @author fscai
 *
 */
public class BasicAccount extends IAccount{
	
	@Override
	public Map<String,String> login() {
		Map<String,String> params = new HashMap<String,String>();
		params.put("url", "http://www.webofknowledge.com");
		params.put("user", this.getUserName());
		params.put("pass", this.getPassword());
	
		Map<String,String> loginMap = HTTPUtils.getCookieUrlAndHtml(this.getLoginUrl(), null ,null, HTTPUtils.POST ,params);
		String url = loginMap.get("url");
		
		if(loginMap.size() == 0 || !url.contains("GeneralSearch_input.do")){
			return null;
		}

		return loginMap;
	}
}
