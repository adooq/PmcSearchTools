package selleck.email.wosaccount;

import java.util.HashMap;
import java.util.Map;

import selleck.utils.HTTPUtils;

/**
 * 使用findlay账号的登陆
 * 
 * @author fscai
 *
 */
public class Findlay extends IAccount {

	@Override
	public Map<String, String> login() {

		String cookie = "JSESSIONID=689576CBDE472D9AE3CA686A1C0ED35E";
		String cookieDomain = "auth.findlay.edu";

		Map<String, String> params = new HashMap<String, String>();
		// params.put("url", "http://www.webofknowledge.com");
		params.put("username", this.getUserName());
		params.put("password", this.getPassword());
		// params.put("lt",
		// "_cD834FC67-95C8-BDF3-8B91-C5CFA22E8B31_kE766EFED-364B-D35B-B959-38DC61BE38B2");
		// params.put("_eventId", "submit");
		// params.put("submit", "登陆");
		Map<String, String> loginMap = HTTPUtils.getCookieUrlAndHtml(this.getLoginUrl(), cookie, cookieDomain,
				HTTPUtils.POST, params);
		String url = loginMap.get("url");

		if (loginMap.size() == 0 || !url.contains("GeneralSearch_input.do")) {
			return null;
		}

		return loginMap;
	}

}
