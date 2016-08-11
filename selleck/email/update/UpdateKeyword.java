package selleck.email.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.Jsoup;

import common.handle.model.Criteria;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.KeywordEmail;
import selleck.email.pojo.PMC;
import selleck.email.pojo.ScienceDirect;
import selleck.email.pojo.Springer;
import selleck.email.pojo.WOS;
import selleck.email.pojo.Wiley;
import selleck.email.service.IBrandTitleTempService;
import selleck.email.service.IKeywordEmailService;
import selleck.email.service.IPMCService;
import selleck.email.service.IScienceDirectService;
import selleck.email.service.ISpringerService;
import selleck.email.service.IWOSService;
import selleck.email.service.IWileyService;
import selleck.email.service.impl.BrandTitleTempServiceImpl;
import selleck.email.service.impl.KeywordEmailServiceImpl;
import selleck.email.service.impl.PMCServiceImpl;
import selleck.email.service.impl.ScienceDirectServiceImpl;
import selleck.email.service.impl.SpringerServiceImpl;
import selleck.email.service.impl.WOSServiceImpl;
import selleck.email.service.impl.WileyServiceImpl;
import selleck.email.update.frame.UpdateKeywordFrame;
import selleck.email.update.parser.ScienceDirectArticleParser;
import selleck.email.update.parser.SpringerArticleParser;
import selleck.email.update.parser.WOSArticleParser;
import selleck.email.update.parser.WileyArticleParser;
import selleck.email.update.template.PMCBaseArticleParser;
import selleck.email.update.tools.HMARobot;
import selleck.email.update.tools.JTextArea4Log;
import selleck.email.update.tools.ParserUtils;
import selleck.email.wosaccount.BasicAccount;
import selleck.email.wosaccount.IAccount;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

/**
 * 更新关键词。根据一个关键词excel，在google scholar里搜，把文章标题保存到brand_title_temp，
 * 然后再去各大文献数据库里用标题搜文章 可以设置更新的期限，wos账号，要搜索的关键词在excel中的范围，间隔等参数。
 * 
 * @author fscai
 *
 */
public class UpdateKeyword {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	private String defaultCookie = "HSID=AEaQNfYjBsdtm_1tD; SSID=AKbYdtY_GI-KP8cWS; APISID=Sr_zm-xezv184VqM/A_qc8rGWVVvMxpEYc; SAPISID=i8-aVvOy7L02UIpm/AngNn8kIlpW1rLTjA; SID=DQAAAKMBAACIxJYZjWtlqAGn_5nddPWpVso2Cxne0r8iiFhR3xiy3McXg7xEu1--3ZZYN00hSckoiHpzGW0-sGjQ_8eHR0refKRFS1ngtEs7BdYYjCtgcoAumPnsogBHusZz-TUd6YJGCqZsed8NA9ajDNtLc1pGYGnY0QRkSTIiWsn_FWeJs9e6J7akcRZ2hYv0rtyKybW2ZGLzgPknoemPv_L3RgJC3XVMu7MpiCJLMfC-1V0JF9j-6fhzQ-x_Trxa14njPFMDT0VRBbw8pJZhxFaTGISi4sC8-x0TlMcMdvC1b449AiNkR-NJp-tXxTfAXCyVXedardRHdih61ypuGPROExtRZcLy3dAI4PsFF2a2L3VuZTG_odSTSExwuK6pheEjnlZKxsNuqG_-UXrlupC6Iz_xkVYX-nvxZEpLLIbjvu32gTX3j6obBdJ9BhL7hdTm2WbIsoY89KJcFrIqxxoqRBk8j3PnjLVMcXnyCOGkGai3jJ9aNHZ9GjejS8ZX8hEQ0WjG1Nea84b3PLDNC8o-xXp39GpTWkMIOWP46TIRKapYvAkv2qeVseTnHpsqD6hYl3I; PREF=ID=1111111111111111:U=43d8662080e9f019:LD=zh-CN:NW=1:TM=1397549951:LM=1434604634:GM=1:S=OH9uHHi6mm3zaNQ4; GSP=NW=1:A=kg7M8A:CPTS=1434618706:LM=1434618706:S=bUtNPh9o8izvPZCt; NID=68=Vc2yBCb9lzn5C87UKzPBmvmtok1iQ1qOoffxgLG6203MgN_XunxPYaJ5B6VmnnQxT49INQvEXDiLBMSC2hNmVCtqZD1GhEXSCKcwPro_61pJQJsvvlJWzJB187Nw9U_907AF6Gt_4JWB65vRODduj4lhmO25Kw";
	private String defaultCookieDomain = ".google.com";
	private IBrandTitleTempService bttService = null;

	IWOSService wosService = null;
	IWileyService wileyService = null;
	IPMCService pmcService = null;
	ISpringerService springerService = null;
	IScienceDirectService sdService = null;
	IKeywordEmailService keService = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final UpdateKeywordFrame frame = UpdateKeywordFrame.getFrame();
		final UpdateKeyword updateKeyword = new UpdateKeyword();
		updateKeyword.loggerTA = frame.getLoggerTA();

		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updateKeyword.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				frame.getStartBT().setEnabled(true);
				updateKeyword.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updateKeyword.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updateKeyword.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					INTERVAL = Integer.valueOf(frame.getIntervalText().getText()) * 1000
							+ new Double(Math.random() * 10000).intValue();
					String excelName = frame.getExcelText().getText();
					String account = frame.getAccountSelect().getSelectedItem().toString();
					String keywordRange = frame.getPubLabel().getText(); // 用来填写关键词excel里需要抓的范围，形如:
																			// 1-100
					String dbName = frame.getDbSelect().getSelectedItem().toString();
					String startYear = frame.getFromYearText().getText();
					String endYear = frame.getToYearText().getText();

					updateKeyword.bttService = new BrandTitleTempServiceImpl(dbName);
					updateKeyword.wosService = new WOSServiceImpl(dbName);
					updateKeyword.wileyService = new WileyServiceImpl(dbName);
					updateKeyword.pmcService = new PMCServiceImpl(dbName);
					updateKeyword.springerService = new SpringerServiceImpl(dbName);
					updateKeyword.sdService = new ScienceDirectServiceImpl(dbName);
					updateKeyword.keService = new KeywordEmailServiceImpl(dbName);

					if (select.equals("GoogleScholar上搜索关键词")) {
						updateKeyword.loadKeywordFromExcel(excelName, startYear, endYear, keywordRange);
					} else if (select.equals("在文献库中搜索文章标题")) {
						updateKeyword.searchTitleIn5DB(account);
					}
					updateKeyword.setStartFlag(false);
					frame.stopMouseClicked();
					updateKeyword.loggerTA.append("======== 完成全部关键词搜索 ========");
				}
			}
		});
		gmailThread.start();
	}

	/**
	 * 按文章标题在5大数据库里搜，顺序是springer , science direct, WOS, wiley , pmc。 因为springer
	 * , science direct文章页面格式比较标准，所以先于WOS抓。
	 * 
	 * @param account
	 */
	public void searchTitleIn5DB(String account) {
		Criteria criteria = new Criteria();
		List<BrandTitleTemp> bbtList;
		int id = 1;
		int maxId = bttService.selectMaxId();

		// 如果文章链接是Springer的，直接访问文章url抓文章信息 http://link.springer.com
		System.out.println("开始在Springer里搜文章");
		loggerTA.append("开始在Springer里搜文章\n");
		while (true) {
			criteria.setWhereClause(" haveread = 0 and INSTR(WEBSITE,'http://link.springer.com') > 0 and id >= " + id
					+ " and id < " + (id + 5000));
			bbtList = bttService.selectByCriteria(criteria);
			for (BrandTitleTemp brandTitleTemp : bbtList) {
				Springer springer = null;
				try {
					String url = java.net.URLDecoder.decode(brandTitleTemp.getWebsite(), "utf-8"); // gogole
																									// scholar上的url还需要处理一下
					url = url.replaceAll("\\s", "");
					Map<String, String> articleMap = HTTPUtils.getCookieUrlAndHtml(url, null, null, HTTPUtils.GET,
							null);

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					String articleHtml = articleMap.get("html");
					if (articleHtml == null || articleHtml.isEmpty()) {
						continue;
					}
					springer = new Springer();
					springer.setType("Article");
					springer.setSearchKeyword(brandTitleTemp.getKeyword());
					springer.setUrl(url);
					springer = SpringerArticleParser.parseFromHTML(articleHtml, springer);
					brandTitleTemp.setHaveFound((byte) 1);

					if (brandTitleTemp.getWebsite().endsWith("fulltext.html")) { // 全文页面
						springer.setFullTextUrl(url);
						springer = SpringerArticleParser.parseFullTextFromHTML(articleHtml, springer);
					} else { // 查看是否包含全文链接，如果有去抓全文页面
								// href="/article/10.1186/2051-1426-2-S2-P1/fulltext.html"
						if (articleHtml.contains(
								"href=\"" + brandTitleTemp.getWebsite().replaceAll("http://link\\.springer\\.com", "")
										+ "/fulltext.html\"")) {
							articleMap = HTTPUtils.getCookieUrlAndHtml(brandTitleTemp.getWebsite() + "/fulltext.html",
									null, null, HTTPUtils.GET, null);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							articleHtml = articleMap.get("html");
							if (articleHtml == null || articleHtml.isEmpty()) {
								continue;
							}

							springer.setFullTextUrl(url + "/fulltext.html");
							springer = SpringerArticleParser.parseFullTextFromHTML(articleHtml, springer);
						}
					}

					loggerTA.append(springer.getTitle() + "\n");

				} catch (Exception e) {
					e.printStackTrace();
					continue;
				} finally {
					try {
						if (springer != null) {
							springerService.saveSpringerByKeyword(springer, brandTitleTemp);
						}
					} catch (Exception ee) {
						ee.printStackTrace();
						continue;
					}
				}
			}
			id += 5000;
			if (id > maxId) {
				break;
			}

		}

		// 如果文章链接是ScienceDirect的，直接访问文章url抓文章信息 http://www.sciencedirect.com
		System.out.println("开始在ScienceDirect里搜文章");
		loggerTA.append("开始在ScienceDirect里搜文章\n");
		HTTPUtils.REFERER = "http://www.sciencedirect.com/";
		id = 1;
		while (true) {
			criteria.setWhereClause(" haveread = 0 and INSTR(WEBSITE,'http://www.sciencedirect.com') > 0 and id >= "
					+ id + " and id < " + (id + 5000));
			bbtList = bttService.selectByCriteria(criteria);
			for (BrandTitleTemp brandTitleTemp : bbtList) {
				ScienceDirect scienceDirect = null;
				try {
					String articleUrl = java.net.URLDecoder.decode(brandTitleTemp.getWebsite(), "utf-8"); // gogole
																											// scholar上的url还需要处理一下
					articleUrl = articleUrl.replaceAll("\\s", "");
					// String cookie =
					// "EUID=e64a9024-631c-11e4-b8a0-00000aacb35f;
					// MIAMISESSION=e63af7f4-631c-11e4-b8a0-00000aacb35f:3592447076;
					// CARS_COOKIE=0228e002b508f7f5a9716e7be42a4415bb35c337a923036d327be68643d45f3a48bbe6f2fd6b2683e4c5b9667a491b3ebc7537968b50ab3a;
					// USER_STATE_COOKIE=346fa8c434beaa18dbb50e2b7e6f20803c2c344c39e22674;
					// TARGET_URL=fcf74dd786744d87fbaaaf8652a764ab4a79b0d3ed681139e910692376063105cb1468c63e712961e8908ef471ab6177d378e78873d526a2;
					// DEFAULT_SESSION_SUBJECT=;
					// sid=538a681a-6969-4b8e-9ce0-05fbf018ea3e;
					// optimizelySegments=%7B%22204775011%22%3A%22ff%22%2C%22204658328%22%3A%22false%22%2C%22204736122%22%3A%22direct%22%2C%22204728159%22%3A%22none%22%7D;
					// optimizelyEndUserId=oeu1414993652634r0.9870375613025336;
					// optimizelyBuckets=%7B%7D; s_cc=true;
					// s_fid=1518D78B022E0AA7-28ACA5E95FFD2A1C;
					// s_nr=1414994274851-New; s_vnum=1417363200365%26vn%3D1;
					// s_invisit=true; s_lv=1414994274852; s_lv_s=First%20Visit;
					// gpv_p19=article; s_ppv=-; s_sq=%5B%5BB%5D%5D;
					// gpv_e2=Explicit; __cp=1414994274666;
					// gpv_art_abs_prd=%3BS0092867414012392;
					// gpv_art_abs_dt=1414994274853";
					// String cookieDomain = ".sciencedirect.com";
					Map<String, String> articleMap = HTTPUtils.getCookieUrlAndHtml(articleUrl, null, null,
							HTTPUtils.GET, null);

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					String articleHtml = articleMap.get("html");
					if (articleHtml == null || articleHtml.isEmpty()) {
						continue;
					}

					// 抓取页面上的SDM.pf.frag.fat参数，
					List<String> fats = ParserUtils.findWithPrefixAndSuffix("SDM.pf.frag.fat = '", "';", articleHtml);
					if (fats.size() > 0) {
						String fat = fats.get(0);
						fat = ParserUtils.trim(fat);

						String articleId = articleUrl.substring(articleUrl.lastIndexOf("/") + 1); // 形如S1876285914003295
						String flagAllUrl = "http://www.sciencedirect.com/science/frag/" + articleId + "/" + fat
								+ "/all";
						// cookie = "EUID=e64a9024-631c-11e4-b8a0-00000aacb35f;
						// MIAMISESSION=e63af7f4-631c-11e4-b8a0-00000aacb35f:3592447157;
						// CARS_COOKIE=0228e002b508f7f5a9716e7be42a4415bb35c337a923036d327be68643d45f3a48bbe6f2fd6b2683e4c5b9667a491b3ebc7537968b50ab3a;
						// USER_STATE_COOKIE=346fa8c434beaa18dbb50e2b7e6f20803c2c344c39e22674;
						// TARGET_URL=fcf74dd786744d87fbaaaf8652a764ab4a79b0d3ed681139e910692376063105cb1468c63e712961e8908ef471ab6177d378e78873d526a2;
						// DEFAULT_SESSION_SUBJECT=;
						// sid=538a681a-6969-4b8e-9ce0-05fbf018ea3e;
						// optimizelySegments=%7B%22204775011%22%3A%22ff%22%2C%22204658328%22%3A%22false%22%2C%22204736122%22%3A%22direct%22%2C%22204728159%22%3A%22none%22%7D;
						// optimizelyEndUserId=oeu1414993652634r0.9870375613025336;
						// optimizelyBuckets=%7B%7D; s_cc=true;
						// s_fid=1518D78B022E0AA7-28ACA5E95FFD2A1C;
						// s_nr=1414994357746-New;
						// s_vnum=1417363200365%26vn%3D1; s_invisit=true;
						// s_lv=1414994357747; s_lv_s=First%20Visit;
						// gpv_p19=article; s_ppv=-; s_sq=%5B%5BB%5D%5D;
						// gpv_e2=Explicit; __cp=1414994357390;
						// gpv_art_abs_prd=%3BS0092867414012392;
						// gpv_art_abs_dt=1414994274853;
						// optimizelyPendingLogEvents=%5B%5D;
						// gpv_art_full_prd=%3BS187628591400343X;
						// gpv_art_full_dt=1414994357748";

						Map<String, String> fragMap = HTTPUtils.getCookieUrlAndHtml(flagAllUrl, null, null,
								HTTPUtils.GET, null);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						String fragHtml = fragMap.get("html");
						if (fragHtml == null || fragHtml.isEmpty()) {
							continue;
						}

						scienceDirect = new ScienceDirect();
						scienceDirect.setSearchKeyword(brandTitleTemp.getKeyword());
						scienceDirect.setUrl(articleUrl);
						scienceDirect = ScienceDirectArticleParser.parsePubFromHTML(articleHtml, scienceDirect);
						scienceDirect = ScienceDirectArticleParser.parseFromHTML(fragHtml, scienceDirect);
						brandTitleTemp.setHaveFound((byte) 1);
						loggerTA.append(scienceDirect.getTitle() + "\n");
					}

				} catch (Exception e) {
					e.printStackTrace();
					continue;
				} finally {
					if (scienceDirect != null) {
						sdService.saveScienceDirectByKeyword(scienceDirect, brandTitleTemp);
					}
				}

			}

			id += 5000;
			if (id > maxId) {
				break;
			}
		}
		HTTPUtils.REFERER = null;
		// 在WOS里按标题搜
		System.out.println("开始在WOS里搜文章");
		loggerTA.append("开始在WOS里搜文章\n");
		// wos login
		id = 1;
		Map<String, String> loginMap = null;
		try {
			loginMap = this.wosLogin(account);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (loginMap == null) {
			return;
		}
		String cookies = loginMap.get("cookie");
		String url = loginMap.get("url");

		while (true) {
			criteria.setWhereClause(" haveread = 0 and id >= " + id + " and id < " + (id + 5000));
			// criteria.setOrderByClause("PICK_DATE desc");
			bbtList = bttService.selectByCriteria(criteria);

			// 在WOS里一次搜25个，但是由于有的标题比较短，会搜到很多文章，在其中找到我们想要的太慢了，这个方法不可行
			// 所以只能一次搜一个标题，取第一条搜到的结果
			// @deprecated
			// List<BrandTitleTemp> temps = new ArrayList<BrandTitleTemp>(25);
			// for (BrandTitleTemp brandTitleTemp : bbtList) {
			// String originalTitle = brandTitleTemp.getOriginalTitle();
			// if (originalTitle == null || originalTitle.trim().isEmpty()) {
			// continue;
			// }
			// temps.add(brandTitleTemp);
			// if (temps.size() >= 25) {
			// try {
			// while(true){
			// boolean success = this.updateByTitleAndRange(url, cookies,
			// temps,"Latest5Years"); // 根据文章标题和时间range去搜索
			// if(success){
			// break;
			// }else{ // 重新登录
			// loginMap = this.wosLogin(account);
			// if(loginMap == null){
			// return;
			// }
			// cookies = loginMap.get("cookie");
			// url = loginMap.get("url");
			// }
			// }
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// continue;
			// } finally {
			// temps.clear();
			// }
			// }
			// }
			// if (temps.size() > 0) {
			// try {
			// this.updateByTitleAndRange(url, cookies, temps,"Latest5Years");
			// // 根据文章标题和时间range去搜索
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }

			// 一次搜一个标题
			List<BrandTitleTemp> temps = new ArrayList<BrandTitleTemp>(1);
			for (BrandTitleTemp brandTitleTemp : bbtList) {
				String originalTitle = brandTitleTemp.getOriginalTitle();
				if (originalTitle == null || originalTitle.trim().isEmpty()) {
					continue;
				}
				temps.add(brandTitleTemp);
				try {
					while (true) {
						boolean success = this.updateByTitleAndRange(url, cookies, temps, "Latest5Years"); // 根据文章标题和时间range去搜索
						if (success) {
							break;
						} else { // 重新登录
							loginMap = this.wosLogin(account);
							if (loginMap == null) {
								return;
							}
							cookies = loginMap.get("cookie");
							url = loginMap.get("url");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				} finally {
					temps.clear();
				}
			}

			id += 5000;
			if (id > maxId) {
				break;
			}
		}

		// 如果文章链接是Wiley的，直接访问文章url抓文章信息
		System.out.println("开始在Wiley里搜文章");
		loggerTA.append("开始在Wiley里搜文章\n");
		id = 1;
		WileyArticleParser wileyParser = new WileyArticleParser();
		while (true) {
			criteria.setWhereClause(
					" havefound = 0  and INSTR(WEBSITE,'http://onlinelibrary.wiley.com') > 0 and  id >= " + id
							+ " and id < " + (id + 5000)); // 搜索Wiley文章
			bbtList = bttService.selectByCriteria(criteria);
			for (BrandTitleTemp brandTitleTemp : bbtList) {
				String articleTitle = brandTitleTemp.getOriginalTitle();
				if (articleTitle.startsWith("Corrigendum") || articleTitle.startsWith("Corrigenda")
						|| articleTitle.startsWith("Errata") || articleTitle.startsWith("Erratum")
						|| articleTitle.startsWith("Preface") || articleTitle.startsWith("Announcement")
						|| articleTitle.startsWith("Table of Contents") || articleTitle.startsWith("Content")
						|| articleTitle.startsWith("Editorial Board") || articleTitle.startsWith("Cover ")
						|| articleTitle.startsWith("Conference Calendar") || articleTitle.startsWith("Correction")
						|| articleTitle.startsWith("Issue Information") || articleTitle.startsWith("Index")
						|| articleTitle.startsWith("Reply") || articleTitle.startsWith("Sponsors")
						|| articleTitle.matches("\\A.{0,10}Abstract.*") || articleTitle.matches("\\A.{0,10}Index.*")
						|| articleTitle.matches("\\A.{0,10}issue.*") || articleTitle.matches("\\A.{0,10}Snippet.*")
						|| articleTitle.startsWith("Inside Cover") || articleTitle.startsWith("Back Cover")) {
					continue;
				}
				Wiley wiley = null;
				try {
					url = brandTitleTemp.getWebsite();

					// 有的时候url会是其他资源，例如pdf之类的，这时尝试去访问abstract页面
					if (!url.endsWith("/abstract") && !url.endsWith("/full") && !url.endsWith("/summary")) {
						url = url.substring(0, url.lastIndexOf("/")) + "/abstract";
					}

					Map<String, String> articleMap = HTTPUtils.getCookieUrlAndHtml(url, null, null, HTTPUtils.GET,
							null);

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					String articleHtml = articleMap.get("html");
					if (articleHtml == null || articleHtml.isEmpty()) {
						continue;
					}

					// 有时候会访问到Wiley的enhanced html，要返回到老版本页面才能解析
					List<String> oldLinks = ParserUtils.findWithPrefixAndSuffix("<a id=\"wol1backlink\" href=\"", "\"",
							articleHtml);
					if (oldLinks.size() > 0) {
						url = oldLinks.get(0);
						articleMap = HTTPUtils.getCookieUrlAndHtml(url, null, null, HTTPUtils.GET, null);
					}

					articleHtml = articleMap.get("html");
					if (articleHtml == null || articleHtml.isEmpty()) {
						continue;
					}

					wiley = new Wiley();
					brandTitleTemp.setHaveFound((byte) 1);
					if (url.endsWith("/summary")) {
						wiley.setType("Book");
					} else {
						wiley.setType("Article");
					}
					wiley.setUrl(url);
					wiley.setFullTextUrl(url);
					wiley.setSearchKeyword(brandTitleTemp.getKeyword());
					wiley = wileyParser.parseFromHTML(articleHtml, wiley);

					// 标识是否免费文章
					// <span class="freeAccess" title="You have free access to
					// this content">You have free access to this content</span>
					List<String> freeSpans = ParserUtils.findInContent(
							"(<span class=\"freeAccess\" title=\"You have free access to this content\">)|(<span class=\"openAccess\" title=\"You have full text access to this OnlineOpen article\">)",
							articleHtml);
					if (!url.endsWith("/full") && freeSpans.size() > 0) { // 免费文章，尝试抓全文
						// /doi/10.1002/9781118647363.ch8/full
						List<String> fullTextUrls = ParserUtils.findInContent(
								wiley.getUrl().substring(0, wiley.getUrl().lastIndexOf("/")) + "/full", articleHtml);
						if (fullTextUrls.size() > 0) {
							wiley.setFullTextUrl(fullTextUrls.get(0));
							Map<String, String> fullArticleMap = HTTPUtils.getCookieUrlAndHtml(wiley.getFullTextUrl(),
									null, null, HTTPUtils.GET, null);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							String fullArticleHtml = fullArticleMap.get("html");
							if (fullArticleHtml != null && !fullArticleHtml.isEmpty()) {
								wiley = wileyParser.parseFullTextFromHTML(fullArticleHtml, wiley);
							}
						}
					} else if (url.endsWith("/full") && freeSpans.size() == 0) { // 链接以/full结尾，但没有免费文章标识的，再去访问文章摘要页面，因为全文页面只是个付费页面什么信息都没有
						List<String> abstractUrls = ParserUtils.findInContent(
								wiley.getUrl().substring(0, wiley.getUrl().lastIndexOf("/")) + "/abstract",
								articleHtml);
						if (abstractUrls.size() > 0) {
							wiley.setFullTextUrl(null);
							wiley.setUrl(abstractUrls.get(0));
							Map<String, String> abstractArticleMap = HTTPUtils.getCookieUrlAndHtml(wiley.getUrl(), null,
									null, HTTPUtils.GET, null);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							String abstractArticleHtml = abstractArticleMap.get("html");
							if (abstractArticleHtml != null && !abstractArticleHtml.isEmpty()) {
								wiley = wileyParser.parseFromHTML(abstractArticleHtml, wiley);
								wiley.setFullText(null);
								wiley.setFullTextUrl(null);
							}
						}
					} else if (!url.endsWith("/full") && freeSpans.size() == 0) {
						wiley.setFullText(null);
						wiley.setFullTextUrl(null);
					}

					loggerTA.append(wiley.getTitle() + "\n");

				} catch (Exception e) {
					e.printStackTrace();
					continue;
				} finally {
					try {
						if (wiley != null) {
							wileyService.saveWileyByKeyword(wiley, brandTitleTemp);
						}
					} catch (Exception ee) {
						ee.printStackTrace();
						continue;
					}
				}
			}

			id += 5000;
			if (id > maxId) {
				break;
			}
		}

		// http://www.ncbi.nlm.nih.gov/pmc/
		// 如果文章链接是pmc的，直接访问文章url抓文章信息
		System.out.println("开始在PMC里搜文章");
		loggerTA.append("开始在PMC里搜文章\n");
		id = 1;
		PMCBaseArticleParser pmcParser = new PMCBaseArticleParser();
		while (true) {
			criteria.setWhereClause(
					" havefound = 0 and INSTR(WEBSITE,'http://www.ncbi.nlm.nih.gov/pmc/') > 0 and  id >= " + id
							+ " and id < " + (id + 5000)); // 搜索pmc文章
			bbtList = bttService.selectByCriteria(criteria);
			for (BrandTitleTemp brandTitleTemp : bbtList) {
				try {
					int i = 1; // 尝试访问次数
					Map<String, String> articleMap;
					String articleHtml;
					while (i <= 2) { // 有时候pmc会封IP，换个IP再尝试一次
						articleMap = HTTPUtils.getCookieUrlAndHtml(brandTitleTemp.getWebsite(), null, null,
								HTTPUtils.GET, null);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;
						}
						articleHtml = articleMap.get("html");
						if (articleHtml == null || articleHtml.isEmpty()
								|| articleHtml.contains("Your access to PubMed Central has been blocked")) {
							HMARobot hma = new HMARobot();
							hma.changeIP(loggerTA);
						} else {
							PMC pmc = new PMC();
							pmc.setUrl(brandTitleTemp.getWebsite());
							pmc.setSearchKeyword(brandTitleTemp.getKeyword());
							pmc = pmcParser.parseFromHTML(articleHtml, pmc);
							loggerTA.append(pmc.getTitle() + "\n");
							brandTitleTemp.setHaveFound((byte) 1);
							pmcService.savePMCByKeyword(pmc, brandTitleTemp);
							break;
						}
						i++;
					}

				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}

			id += 5000;
			if (id > maxId) {
				break;
			}
		}

		// 如果5大库里都没有找到该标题文章，直接访问该页面，抓一个email
		System.out.println("直接在网页里搜文章");
		loggerTA.append("直接在网页里搜文章\n");
		id = 1;
		while (true) {
			criteria.setWhereClause(" haveread = 0 and  id >= " + id + " and id < " + (id + 5000));
			bbtList = bttService.selectByCriteria(criteria);
			for (BrandTitleTemp brandTitleTemp : bbtList) {
				Map<String, String> articleMap = HTTPUtils.getCookieUrlAndHtml(brandTitleTemp.getWebsite(), null, null,
						HTTPUtils.GET, null);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				if (articleMap.get("html") == null || articleMap.get("html").trim().isEmpty()) {
					continue;
				}

				Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
				Matcher matcher = p.matcher(articleMap.get("html"));
				while (matcher.find()) {
					KeywordEmail keywordEmail = new KeywordEmail();
					keywordEmail.setEmail(matcher.group());
					keywordEmail.setKeyword(brandTitleTemp.getKeyword());
					keywordEmail.setUrl(brandTitleTemp.getWebsite());
					keService.saveKeywordEmail(keywordEmail);
					System.out.println("搜索到邮箱 " + keywordEmail.getEmail());
					loggerTA.append("搜索到邮箱 " + keywordEmail.getEmail() + "\n");
				}

			}

			id += 5000;
			if (id > maxId) {
				break;
			}
		}
	}

	/**
	 * 
	 */
	public void loadKeywordFromExcel(String excel, String startYear, String endYear, String keywordRange) {
		Workbook keywordWB = null;
		Workbook pubWB = null;
		try {
			pubWB = WorkbookFactory.create(new File("wos all publications list processed.xlsx"));
			Sheet pubSheet = pubWB.getSheetAt(0);
			int pubRowCount = pubSheet.getLastRowNum();
			int pubStart = Integer.valueOf(keywordRange.split("-")[0]) - 1;
			int pubEnd = Integer.valueOf(keywordRange.split("-")[1]) - 1;
			pubEnd = Math.min(pubEnd, pubRowCount);

			keywordWB = WorkbookFactory.create(new File(excel));
			Sheet sheet = keywordWB.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();

			for (int i = pubStart; i <= pubEnd; i++) { // row从0开始
				Row pubRow = pubSheet.getRow(i);
				String pubName = pubRow.getCell(0).getStringCellValue().trim(); // 期刊名
				loggerTA.append("开始搜索第" + (i + 1) + "个期刊" + pubName);
				for (int j = 0; j < rowCount; j++) { // row从0开始
					Row row = sheet.getRow(j);
					String keyword = row.getCell(0).getStringCellValue().trim(); // 关键词
					updateByKeywordAndRange(keyword, startYear, endYear, pubName);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 2014 finished
	// 2012 journal of biomolecular screening 1221 -- running
	// 2015 CRYSTAL GROWTH & DESIGN 944 -- running
	// 2013 finished
	// 2011 finished

	public void updateByKeywordAndRange(String keyword, String startYear, String endYear, String pubName) {
		// https://scholar.google.com/scholar?
		// as_q=&as_epq=screening+library&as_oq=&as_eq=&as_occt=any&as_sauthors=&as_publication=&as_ylo=2011&as_yhi=2015&btnG=&hl=zh-CN&as_sdt=1%2C5&as_vis=1
		// https://scholar.google.com/scholar?
		// q=%22screening+library%22&hl=zh-CN&as_sdt=1,5&as_ylo=2011&as_yhi=2015&as_vis=1
		// https://scholar.google.com/scholar?start=20&q=%22screening+library%22&hl=zh-CN&as_sdt=1,5&as_ylo=2011&as_yhi=2015&as_vis=1
		// 在google scholar首页查询关键词，主要是为了获取cookie

		// 如果期刊只有一个单词，一年一年地搜；如果不是，所有年份合在一起搜
		if (pubName.trim().contains(" ")) {
			this.loggerTA.append("搜索" + keyword + "在期刊" + pubName + "中 " + startYear + "-" + endYear + "\n");
			queryKeyword(keyword, startYear, endYear, pubName);
		} else {
			int startYearInt = Integer.valueOf(startYear);
			int endYearInt = Integer.valueOf(endYear);
			while (startYearInt <= endYearInt) {
				String currentYear = String.valueOf(startYearInt);
				this.loggerTA.append("搜索" + keyword + "在期刊" + pubName + "中 " + currentYear + "-" + currentYear + "\n");
				queryKeyword(keyword, currentYear, currentYear, pubName);
				startYearInt++;
			}

		}

	}

	/**
	 * 
	 * @param keyword
	 * @param startYear
	 * @param endYear
	 * @param pubName
	 */
	public void queryKeyword(String keyword, String startYear, String endYear, String pubName) {
		String url = "";
		int page = 0;
		String html = "";
		while (true) {
			try {
				try {
					// https://scholar.google.com/scholar?q=%22high+throughput+screening%22&hl=zh-CN&as_publication=biological+chemistry&as_sdt=1,5&as_ylo=2011&as_vis=1
					url = "https://scholar.google.com/scholar?q=" + java.net.URLEncoder.encode(keyword, "utf-8")
							+ "&hl=zh-CN&as_publication=" + java.net.URLEncoder.encode(pubName, "utf-8")
							+ "&as_sdt=1,5&as_ylo=" + startYear + "&as_yhi=" + endYear + "&as_vis=1" + "&num=20";
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return;
				}
				Map<String, String> queryPageMap = HTTPUtils.getCookieUrlAndHtml(url, defaultCookie,
						defaultCookieDomain, HTTPUtils.GET, null);
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				String cookie = queryPageMap.get("cookie");
				html = queryPageMap.get("html");

				if (html != null && html.contains("条结果")) { // 找到约 1,500 条结果 获得
															// 9 条结果
					Pattern p = Pattern.compile(" [0-9,]+ 条结果");
					Matcher matcher = p.matcher(html);
					if (matcher.find()) {
						int count = Integer
								.valueOf(matcher.group().replaceAll(" ", "").replaceAll("条结果", "").replaceAll(",", ""));
						page = new Double(Math.ceil(count / 20.0)).intValue();
						System.out.println("搜到" + page + "条结果");
					}
					break;
				} else if (html != null && html.contains("所有文章均不相符。")) { // 您的搜索
																			// -
																			// "high
																			// throughput
																			// screening"
																			// -
																			// 与
																			// anaesthesia
																			// 在
																			// 2015
																			// 和
																			// 2015
																			// 期间刊登的所有文章均不相符。
					page = 0;
					break;
				} else {
					// 查询有异常，换个IP和cookie
					HMARobot hma = new HMARobot();
					loggerTA.append("Failed: " + keyword + " " + startYear + "-" + endYear + "\n");
					hma.changeIP(loggerTA);

					// 在google scholar首页查询关键词，主要是为了获取cookie
					try {
						url = "https://scholar.google.com/scholar?hl=zh-CN&q="
								+ java.net.URLEncoder.encode(keyword, "utf-8") + "&btnG=&lr=&num=20";
						Map<String, String> homePageMap = HTTPUtils.getCookieUrlAndHtml(url, defaultCookie,
								defaultCookieDomain, HTTPUtils.GET, null);
						try {
							Thread.sleep(INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						defaultCookie = homePageMap.get("cookie");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		// 开始翻页
		if (page != 0) {
			saveBrandTitleTemp(html, keyword); // 抓取第一页，现成的
			for (int i = 1; i < page; i++) {
				this.loggerTA.append(
						"搜索" + keyword + "在期刊" + pubName + "中 " + startYear + "-" + endYear + " 第" + (i + 1) + "页\n");
				// https://scholar.google.com/scholar?start=10&q=%22high+throughput+screening%22&hl=zh-CN&as_publication=biological+chemistry&as_sdt=1,5&as_ylo=2011&as_vis=1
				try {
					url = "https://scholar.google.com/scholar?start=" + i * 20 + "&q="
							+ java.net.URLEncoder.encode(keyword, "utf-8") + "&hl=zh-CN&as_publication="
							+ java.net.URLEncoder.encode(pubName, "utf-8") + "&as_sdt=1,5&as_ylo=" + startYear
							+ "&as_yhi=" + endYear + "&as_vis=1&num=20";
					Map<String, String> homePageMap = HTTPUtils.getCookieUrlAndHtml(url, defaultCookie,
							defaultCookieDomain, HTTPUtils.GET, null);
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					defaultCookie = homePageMap.get("cookie");
					html = homePageMap.get("html");
					if (html != null && html.contains("条结果")) {
						saveBrandTitleTemp(html, keyword);
					} else {
						// 查询有异常，换个IP和cookie
						HMARobot hma = new HMARobot();
						loggerTA.append(keyword + " " + startYear + "-" + endYear + "\n");
						hma.changeIP(loggerTA);

						// 在google scholar首页查询关键词，主要是为了获取cookie
						url = "https://scholar.google.com/scholar?hl=zh-CN&q="
								+ java.net.URLEncoder.encode(keyword, "utf-8") + "&btnG=&lr=&num=20";
						homePageMap = HTTPUtils.getCookieUrlAndHtml(url, defaultCookie, defaultCookieDomain,
								HTTPUtils.GET, null);
						try {
							Thread.sleep(INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						defaultCookie = homePageMap.get("cookie");

						i--; // 为了抵消i++，重新抓这页
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					continue;
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	private void saveBrandTitleTemp(String html, String keyword) {
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<h3 class=\"gs_rt\">", "</h3>", html);
		for (String t : titles) {
			List<String> originalTitles = ParserUtils.findWithPrefixAndSuffix("<a href=\".+?>", "</a>", t);
			if (originalTitles.isEmpty()) {
				continue;
			}
			String originalTitle = originalTitles.get(0);
			originalTitle = Jsoup.parse(originalTitle).text(); // 去html标签
																// ,去换行\t\n\x0B\f\r
			originalTitle = StringEscapeUtils.unescapeHtml4(originalTitle); // 去转义字符，
																			// &gt;
																			// 转换成>符号
			originalTitle = ParserUtils.trim(originalTitle);

			String website = null;
			List<String> websites = ParserUtils.findWithPrefixAndSuffix("href=\"", "\"", t);
			if (!websites.isEmpty()) {
				website = websites.get(0);
			}

			BrandTitleTemp btt = new BrandTitleTemp();
			btt.setKeyword(keyword);
			btt.setOriginalTitle(originalTitle);
			btt.setWebsite(website);
			btt.setPickDate(new Timestamp(System.currentTimeMillis()));
			bttService.insert(btt);

		}
	}

	// /**
	// * 读取期刊的excel，根据期刊名一个个去WOS搜索文章。
	// * @param excel 纯excel文件名，不需要路径， 形如： wos.xlsx
	// * @param range 时间跨度 4week等
	// * @param account 账号名
	// * @param pubRange 期刊在excel里的范围 形如: 1-200
	// */
	// public void loadPubFromExcel(String excel ,String range, String account,
	// String pubRange){
	// Workbook wb = null;
	// try {
	// Map<String,String> loginMap = this.loginAccount(account);
	// try {
	// Thread.sleep(INTERVAL);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// if(loginMap == null){
	// System.out.println("登陆失败");
	// this.getLoggerTA().append("登陆失败\n");
	// return;
	// }
	// System.out.println("登陆成功");
	// this.getLoggerTA().append("登陆成功\n");
	//
	// String cookies = loginMap.get("cookie");
	// String url = loginMap.get("url");
	//
	// wb = WorkbookFactory.create(new File(excel));
	// Sheet sheet = wb.getSheetAt(0);
	// int rowCount = sheet.getLastRowNum();
	// int pubStart = Integer.valueOf(pubRange.split("-")[0]) - 1;
	// int pubEnd = Integer.valueOf(pubRange.split("-")[1]) - 1;
	// pubEnd = Math.min(pubEnd, rowCount);
	// List<String> pubNames = new ArrayList<String>(25); //
	// 一次搜索25个期刊，减少查询次数，貌似查询请求太多会封号。
	// // for (int i = 0; i <= 0; i++) { // for test
	// for (int i = pubStart; i <= pubEnd; i++) { // row从0开始
	// // for (int i = 0; i <= rowCount; i++) { // row从0开始
	// Row row = sheet.getRow(i);
	// String pubName = row.getCell(0).getStringCellValue().trim(); // 期刊名
	// pubNames.add(pubName);
	// if(pubNames.size() >= 25){
	// try {
	// this.updateByPubAndRange(url, cookies, pubNames, range); //
	// 根据期刊名和时间range去搜索
	// } catch (Exception e) {
	// e.printStackTrace();
	// continue;
	// }finally{
	// pubNames.clear();
	// }
	// }
	// }
	// if(pubNames.size() > 0){
	// try {
	// this.updateByPubAndRange(url, cookies, pubNames, range); //
	// 根据期刊名和时间range去搜索
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// } catch (InvalidFormatException e1) {
	// this.loggerTA.append("excel文件格式不正确 \n");
	// e1.printStackTrace();
	// } catch (IOException e1) {
	// this.loggerTA.append("excel文件访问出错 \n");
	// e1.printStackTrace();
	// return;
	// }
	//
	// }

	// public void loadPubFromCSV(String csvName, String range, String account){
	// BufferedReader br = null;
	// try {
	// File csv = new File(csvName); // CSV文件
	// if (!csv.exists()) {
	// this.loggerTA.append("CSV 文件不存在 \n");
	// return;
	// }
	//
	// Map<String,String> loginMap = this.loginAccount(account);
	// try {
	// Thread.sleep(INTERVAL);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// if(loginMap == null){
	// System.out.println("登陆失败");
	// this.getLoggerTA().append("登陆失败\n");
	// return;
	// }
	// System.out.println("登陆成功");
	// this.getLoggerTA().append("登陆成功\n");
	//
	// String cookies = loginMap.get("cookie");
	// String url = loginMap.get("url");
	//
	// br = new BufferedReader(new FileReader(csv));
	// // 读取直到最后一行
	// String line = "";
	// List<String> pubNames = new ArrayList<String>(25);
	// while ((line = br.readLine()) != null) {
	// pubNames.add(line);
	// try{
	// if(pubNames.size() >= 25){
	// this.updateByPubAndRange(url, cookies, pubNames, range); //
	// 根据期刊名和时间range去搜索
	// pubNames.clear();
	// }
	// }catch(Exception e){
	// e.printStackTrace();
	// continue;
	// }
	// }
	// if(pubNames.size() > 0){
	// this.updateByPubAndRange(url, cookies, pubNames, range); //
	// 根据期刊名和时间range去搜索
	// }
	// } catch (FileNotFoundException e) {
	// this.loggerTA.append("CSV 文件不存在 \n");
	// e.printStackTrace();
	// } catch (IOException e) {
	// this.loggerTA.append("excel文件访问出错 \n");
	// e.printStackTrace();
	// } finally {
	// if (br != null) {
	// try {
	// br.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// }

	/**
	 * 登陆WOS账号
	 * 
	 * @param account
	 *            账号名
	 * @return Map<String,String> 有三个key，返回url , cookie , html
	 */
	public Map<String, String> loginAccount(String account) {
		Properties p = new Properties();
		try {
			p.load(new BufferedInputStream(new FileInputStream("wos accounts.properties")));
		} catch (FileNotFoundException e) {
			this.getLoggerTA().append("账号文件读取失败\n");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			this.getLoggerTA().append("账号文件读取失败\n");
			e.printStackTrace();
			return null;
		}

		IAccount wosAccount = null;
		try {
			String firstChar = account.substring(0, 1);
			String className = account.replaceFirst(firstChar, firstChar.toUpperCase()); // account
																							// 首字母大写
			Class<? extends IAccount> accountClass = (Class<? extends IAccount>) Class
					.forName("selleck.email.wosaccount." + className);
			wosAccount = accountClass.newInstance();
		} catch (ClassNotFoundException e1) {
			// e1.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		if (wosAccount == null) {
			wosAccount = new BasicAccount();
		}

		wosAccount.setLoginUrl(p.getProperty(account + ".loginUrl"));
		wosAccount.setUserName(p.getProperty(account + ".user"));
		wosAccount.setPassword(p.getProperty(account + ".pass"));

		return wosAccount.login();
	}

	/**
	 * 按期刊条件搜索wos文章，保存到表search_wos_by_publication
	 * 
	 * @param url
	 *            登陆后获得的url
	 * @param cookies
	 *            登陆后获得的cookie
	 * @param titles
	 *            期刊名
	 * @param range
	 *            时间 4week等
	 * @param keyword
	 *            在googlescholar中查询的关键词
	 * @return 是否查询成功
	 */
	public boolean updateByTitleAndRange(String url, String cookies, List<BrandTitleTemp> brandTitleTemps,
			String range) {
		String sid = "";
		String domain = "";
		String cookieDomain = "";

		Pattern p;
		Matcher matcher;

		// 选择wos核心库，把url中的product=UA替换成product=WOS
		// http://lib-proxy.pnc.edu:2311/UA _GeneralSearch_input.do?product=UA
		// &SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch
		// 转换成 --->
		// http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch_input.do?product=WOS&SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch
		url = url.replaceAll("product=UA", "product=WOS").replaceAll("UA _GeneralSearch_input",
				"WOS_GeneralSearch_input");

		// 获取url里的SID e.g "4Ff3v7CxaSyzVBaFefX"
		p = Pattern.compile("&SID=(\\w)+&");
		matcher = p.matcher(url);
		if (matcher.find()) {
			sid = matcher.group().replaceAll("&SID=", "").replaceAll("&", "");
		}

		// 获取url里的domain e.g "http://lib-proxy.pnc.edu:2311"
		p = Pattern.compile("http[s]?://(.)+?/");
		matcher = p.matcher(url);
		if (matcher.find()) {
			domain = matcher.group().replaceAll("/\\z", "");
			cookieDomain = domain.replaceAll("http://", "").replaceAll(":\\d+", "");
			cookieDomain = cookieDomain.substring(cookieDomain.indexOf(".")); // 把domain的第一个次级域名去掉作为cookie的domain
		}
		// System.out.println("sid: "+ sid);
		// System.out.println("domain: "+ domain);
		// System.out.println("cookieDomain: "+ cookieDomain); // 形如 ".pnc.edu"

		// 发送查询请求
		String searchUrl = domain + "/WOS_GeneralSearch.do"; // http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch.do
		List<String> titles = new ArrayList<String>(brandTitleTemps.size());
		for (BrandTitleTemp btt : brandTitleTemps) {
			titles.add(charaterDeal(btt.getOriginalTitle())); // 去掉一些特殊符号，代码从原来Email_Capture中复制过来的，不明白为什么要这么做
		}
		System.out.println("searching... " + titles.get(0));
		this.loggerTA.append("searching... " + titles.get(0) + "\n");
		Map<String, String> params = getRangeParameter(sid, titles, domain, range);
		Map<String, String> searchMap = HTTPUtils.getCookieUrlAndHtml(searchUrl, cookies, cookieDomain, HTTPUtils.POST,
				params);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cookies = searchMap.get("cookie");
		url = searchMap.get("url");
		String htmlStr = searchMap.get("html");
		if (htmlStr == null || htmlStr.isEmpty()) {
			return false;
		}

		// 在页面上获取qid
		String parentQid = "1";
		p = Pattern.compile("name=\"parentQid\" value=\"\\d+\"");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			parentQid = matcher.group().replaceAll("name=\"parentQid\" value=\"", "").replaceAll("\"", "");
		}

		// 获取url里的SID e.g "4Ff3v7CxaSyzVBaFefX"
		p = Pattern.compile("&SID=(\\w)+&");
		matcher = p.matcher(url);
		if (matcher.find()) {
			sid = matcher.group().replaceAll("&SID=", "").replaceAll("&", "");
		}

		// 发送refine请求，过滤出 articel review letter 三个类型的文章
		String refineUrl = domain + "/Refine.do"; // http://lib-proxy.pnc.edu:2311/Refine.do
		Map<String, String> refineMap = HTTPUtils.getCookieUrlAndHtml(refineUrl, cookies, cookieDomain, HTTPUtils.POST,
				getRefineParams(sid, parentQid));
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		htmlStr = refineMap.get("html");

		// id="trueFinalResultCount">612</span>
		int count = 0; // 文章总数
		p = Pattern.compile("id=\"trueFinalResultCount\">(\\d)+</span>");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			String countStr = matcher.group().replaceAll("id=\"trueFinalResultCount\">", "").replaceAll("</span>", "");
			if (countStr.matches("\\d+")) {
				count = Integer.valueOf(countStr);
			}
		} else if (htmlStr.contains("检索后没有发现记录。") || htmlStr.contains("基本检索")) {
			System.out.println("文章总数:" + count);
			this.loggerTA.append("文章总数:" + count + "\n");
			bttService.setRead(brandTitleTemps.get(0));
			System.out.println("no result");
			System.out.println(htmlStr);
			return true;
		} else { // 页面异常，重新登录
			System.out.println("page error");
			System.out.println(htmlStr);
			return false;
		}
		System.out.println("文章总数:" + count);
		this.loggerTA.append("文章总数:" + count + "\n");

		String articleUrl = ""; // 形如:
								// /full_record.do?product=WOS&search_mode=GeneralSearch&qid=1&SID=4FAffivmBEE7pthQi3e&page=1&doc=
		p = Pattern.compile("/full_record.do?(.)+doc=");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			articleUrl = matcher.group();
			articleUrl = StringEscapeUtils.unescapeHtml4(articleUrl);
		}

		// 一篇一篇抓取文章,文章url = articleUrl + 序号
		// e.g
		// /full_record.do?product=WOS&amp;search_mode=GeneralSearch&amp;qid=1&amp;SID=4FAffivmBEE7pthQi3e&amp;page=1&amp;doc=6

		// for(int i = 1; i <= count;i++){
		for (int i = 1; i <= 1; i++) { // 只抓第一篇
			// String aUrl = StringEscapeUtils.unescapeHtml4(articleUrl + i);
			String aUrl = domain + articleUrl + i;
			Map<String, String> articleMap = HTTPUtils.getCookieUrlAndHtml(aUrl, cookies, cookieDomain, HTTPUtils.GET,
					null);
			if (articleMap.get("html") != null && !articleMap.get("html").isEmpty()) {
				try {
					String articleHtml = articleMap.get("html");
					WOS wos = WOSArticleParser.parseWOSFromHTML(articleHtml);
					wos.setSearchKeyword(brandTitleTemps.get(0).getKeyword());
					brandTitleTemps.get(0).setHaveFound((byte) 1);
					wosService.saveWOSByKeyword(wos, brandTitleTemps.get(0));

					// BrandTitleTemp matchedBtt = findInTitles(wos,
					// brandTitleTemps);
					// if(matchedBtt != null){
					// System.out.println("title: " + wos.getTitle());
					// this.loggerTA.append(wos.getTitle()+"\n");
					// wosService.saveWOSByKeyword(wos,matchedBtt);
					// }else{
					// System.out.println("not matched title: " +
					// wos.getTitle());
					// }
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			// System.out.println("articleHtml " + articleHtml);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/*
	 * @deprecated public void updateWOS(){ Organization o = new Organization();
	 * 
	 * // ashland o.setLoginUrl("http://proxy.ashland.edu:2048/login");
	 * o.setUserName("1164663"); o.setPassword("Garcia");
	 * 
	 * // login Map<String,String> params = new HashMap<String,String>(); //
	 * params.put("url", ""); params.put("url",
	 * "http://www.webofknowledge.com"); params.put("user", o.getUserName());
	 * params.put("pass", o.getPassword());
	 * 
	 * // params.put("user.email", o.getUserName()); //
	 * params.put("user.password", o.getPassword()); // params.put("userIP",
	 * "116.231.109.148"); // params.put("formerurl",
	 * "http://www.selleckchem.com/index.html");
	 * 
	 * Map<String,String> loginMap =
	 * HTTPUtils.getCookieUrlAndHtml(o.getLoginUrl(), null ,null, HTTPUtils.POST
	 * ,params); String cookies = loginMap.get("cookie"); String url =
	 * loginMap.get("url"); System.out.println("final url: " + url);
	 * System.out.println("final cookies: " + cookies); // System.out.println(
	 * "final html: " + loginMap.get("html"));
	 * 
	 * if(loginMap.size() == 0 || !url.contains("GeneralSearch_input.do")){
	 * System.out.println("登陆失败"); return; }
	 * 
	 * String sid = ""; String domain = ""; String cookieDomain = "";
	 * 
	 * Pattern p; Matcher matcher;
	 * 
	 * // 选择wos核心库，把url中的product=UA替换成product=WOS //
	 * http://lib-proxy.pnc.edu:2311/UA _GeneralSearch_input.do?product=UA
	 * &SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch // 转换成 ---> //
	 * http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch_input.do?product=WOS&SID=
	 * 4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch url =
	 * url.replaceAll("product=UA", "product=WOS").replaceAll(
	 * "UA _GeneralSearch_input", "WOS_GeneralSearch_input");
	 * 
	 * // 获取url里的SID e.g "4Ff3v7CxaSyzVBaFefX" p =
	 * Pattern.compile("&SID=(\\w)+&"); matcher = p.matcher(url); if
	 * (matcher.find()) { sid = matcher.group().replaceAll("&SID=",
	 * "").replaceAll("&", ""); }
	 * 
	 * // 获取url里的domain e.g "http://lib-proxy.pnc.edu:2311" p =
	 * Pattern.compile("http[s]?://(.)+?/"); matcher = p.matcher(url); if
	 * (matcher.find()) { domain = matcher.group().replaceAll("/\\z", "");
	 * cookieDomain = domain.replaceAll("http://","").replaceAll(":\\d+", "");
	 * cookieDomain = cookieDomain.substring(cookieDomain.indexOf(".")); }
	 * System.out.println("sid: "+ sid); System.out.println("domain: "+ domain);
	 * System.out.println("cookieDomain: "+ cookieDomain); // 形如 ".pnc.edu"
	 * 
	 * String pulicationName = "SCIENCE"; // 发送查询请求 String searchUrl = domain +
	 * "/WOS_GeneralSearch.do"; //
	 * http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch.do params =
	 * getRangeParameter(sid, pulicationName,domain, "4week");
	 * Map<String,String> searchMap = HTTPUtils.getCookieUrlAndHtml(searchUrl,
	 * cookies ,cookieDomain, HTTPUtils.POST ,params);
	 * 
	 * cookies = searchMap.get("cookie"); url = searchMap.get("url"); String
	 * htmlStr = searchMap.get("html");
	 * 
	 * // 在页面上获取qid String parentQid = "1"; p = Pattern.compile(
	 * "name=\"parentQid\" value=\"\\d+\""); matcher = p.matcher(htmlStr); if
	 * (matcher.find()) { parentQid = matcher.group().replaceAll(
	 * "name=\"parentQid\" value=\"", "").replaceAll("\"", ""); }
	 * 
	 * // 获取url里的SID e.g "4Ff3v7CxaSyzVBaFefX" p =
	 * Pattern.compile("&SID=(\\w)+&"); matcher = p.matcher(url); if
	 * (matcher.find()) { sid = matcher.group().replaceAll("&SID=",
	 * "").replaceAll("&", ""); }
	 * 
	 * 
	 * // 发送refine请求，过滤出 articel review letter 三个类型的文章 String refineUrl = domain
	 * + "/Refine.do"; // http://lib-proxy.pnc.edu:2311/Refine.do
	 * Map<String,String> refineMap = HTTPUtils.getCookieUrlAndHtml(refineUrl,
	 * cookies ,cookieDomain, HTTPUtils.POST ,getRefineParams(sid, parentQid));
	 * htmlStr = refineMap.get("html");
	 * 
	 * // id="trueFinalResultCount">612</span> int count = 0; // 文章总数 p =
	 * Pattern.compile("id=\"trueFinalResultCount\">(\\d)+</span>"); matcher =
	 * p.matcher(htmlStr); if (matcher.find()) { String countStr =
	 * matcher.group().replaceAll("id=\"trueFinalResultCount\">",
	 * "").replaceAll("</span>", ""); if(countStr.matches("\\d+")){ count =
	 * Integer.valueOf(countStr); } } System.out.println("文章总数:"+count);
	 * 
	 * String articleUrl = ""; // 形如:
	 * /full_record.do?product=WOS&search_mode=GeneralSearch&qid=1&SID=
	 * 4FAffivmBEE7pthQi3e&page=1&doc= p =
	 * Pattern.compile("/full_record.do?(.)+doc="); matcher =
	 * p.matcher(htmlStr); if (matcher.find()) { articleUrl = matcher.group();
	 * articleUrl = StringEscapeUtils.unescapeHtml4(articleUrl); }
	 * 
	 * // 一篇一篇抓取文章,文章url = articleUrl + 序号 // e.g
	 * /full_record.do?product=WOS&amp;search_mode=GeneralSearch&amp;qid=1&amp;
	 * SID=4FAffivmBEE7pthQi3e&amp;page=1&amp;doc=6
	 * 
	 * for(int i = 1; i <= count;i++){ // for(int i = 2; i <= 2 ; i++){ // for
	 * test // String aUrl = StringEscapeUtils.unescapeHtml4(articleUrl + i);
	 * String aUrl = domain + articleUrl + i; String articleHtml =
	 * HTTPUtils.getCookieUrlAndHtml(aUrl, cookies ,cookieDomain , HTTPUtils.GET
	 * ,null).get("html"); // System.out.println("articleHtml " + articleHtml);
	 * WOS wos = WOSArticleParser.parseWOSFromHTML(articleHtml);
	 * System.out.println("title: " + wos.getTitle()); wosService.saveWOS(wos);
	 * }
	 * 
	 * System.out.println("finished"); }
	 */

	/**
	 * 
	 * @param sid
	 *            url里的SID
	 * @param pNames
	 *            要查询的期刊名
	 * @param domain
	 *            主域名 http://lib-proxy.pnc.edu:2311
	 * @param startYear
	 *            起始年 2009
	 * @param endYear
	 *            结束年 2014
	 * @return
	 */
	public static Map<String, String> getYearParameter(String sid, List<String> pNames, String domain, String startYear,
			String endYear) {
		Map<String, String> params = getBaseHttpRequestParameter(sid, pNames, domain);
		params.put("range", "ALL");
		params.put("period", "Year Range");
		params.put("startYear", startYear);
		params.put("endYear", endYear);
		return params;
	}

	/**
	 * 设置查询请求的post body，特别
	 * 
	 * @param sid
	 *            url里的SID
	 * @param titles
	 *            要查询的文章标题
	 * @param domain
	 *            主域名 http://lib-proxy.pnc.edu:2311
	 * @param range
	 *            时间 4week YearToDate 等
	 * @return
	 */
	public static Map<String, String> getRangeParameter(String sid, List<String> titles, String domain, String range) {
		Map<String, String> params = getBaseHttpRequestParameter(sid, titles, domain);
		params.put("range", range);
		params.put("period", "Range Selection");
		params.put("startYear", "1992");
		params.put("endYear", "2014");
		return params;
	}

	/**
	 * 设置查询请求的post body，包含大多数通用参数。
	 * 
	 * @param sid
	 *            url里的SID
	 * @param titles
	 *            要查询的标题
	 * @param domain
	 *            主域名 http://lib-proxy.pnc.edu:2311
	 * @return
	 */
	private static Map<String, String> getBaseHttpRequestParameter(String sid, List<String> titles, String domain) {
		Map<String, String> reqParam = new IdentityHashMap<String, String>();
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// String sysEndDate = sdf.format(new Date());
		// SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy");
		// String year = sdf2.format(new Date());

		reqParam.put("fieldCount", String.valueOf(titles.size()));
		reqParam.put("action", "search");
		reqParam.put("product", "WOS");
		reqParam.put("search_mode", "GeneralSearch");
		reqParam.put("SID", sid); // SID in url
		reqParam.put("max_field_count", "25");

		/*
		 * not urlDecoded yet reqParam.put("max_field_notice",
		 * "%E6%B3%A8%E6%84%8F%3A+%E6%97%A0%E6%B3%95%E6%B7%BB%E5%8A%A0%E5%8F%A6%E4%B8%80%E5%AD%97%E6%AE%B5%E3%80%82"
		 * ); // unknown param reqParam.put("input_invalid_notice",
		 * "%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A+%E8%AF%B7%E8%BE%93%E5%85%A5%E6%A3%80%E7%B4%A2%E8%AF%8D%E3%80%82"
		 * ); reqParam.put("exp_notice",
		 * "%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A+%E4%B8%93%E5%88%A9%E6%A3%80%E7%B4%A2%E8%AF%8D%E5%8F%AF%E5%9C%A8%E5%A4%9A%E4%B8%AA%E5%AE%B6%E6%97%8F%E4%B8%AD%E6%89%BE%E5%88%B0+%28"
		 * ); // unknown param reqParam.put("input_invalid_notice_limits",
		 * "+%3Cbr%2F%3E%E6%B3%A8%3A+%E6%BB%9A%E5%8A%A8%E6%A1%86%E4%B8%AD%E6%98%BE%E7%A4%BA%E7%9A%84%E5%AD%97%E6%AE%B5%E5%BF%85%E9%A1%BB%E8%87%B3%E5%B0%91%E4%B8%8E%E4%B8%80%E4%B8%AA%E5%85%B6%E4%BB%96%E6%A3%80%E7%B4%A2%E5%AD%97%E6%AE%B5%E7%9B%B8%E7%BB%84%E9%85%8D%E3%80%82"
		 * ); // unknown param reqParam.put("sa_params",
		 * "WOS%7C%7C1ByB9OWWlCEAOeYCKIn%7Chttp%3A%2F%2Flib-proxy.pnc.edu%3A2311%7C%27&"
		 * );
		 */

		// urlDecoded
		reqParam.put("max_field_notice", "注意: 无法添加另一字段。"); // unknown param
		reqParam.put("input_invalid_notice", "请输入检索词。"); // unknown param
		reqParam.put("exp_notice", "检索错误: 专利检索词可在多个家族中找到 ("); // unknown param
		reqParam.put("input_invalid_notice_limits", "滚动框中显示的字段必须至少与一个其他检索字段相组配。"); // unknown
																					// param
		reqParam.put("sa_params", "WOS||" + sid + "|" + domain + "|'");

		reqParam.put("formUpdated", "true");

		reqParam.put("value(input1)", "\"" + titles.get(0) + "\"");
		// reqParam.put("value(select1)","SO"); // may be publication select
		reqParam.put("value(select1)", "TI"); // may be "标题" select
		reqParam.put("value(hidInput1)", "");
		// reqParam.put("value(bool_1_2)","OR");
		for (int i = 1; i < titles.size(); i++) {
			reqParam.put("value(bool_" + i + "_" + (i + 1) + ")", "OR");
			reqParam.put("value(input" + (i + 1) + ")", "\"" + titles.get(i) + "\"");
			// reqParam.put("value(select"+(i+1)+")","SO"); // may be
			// publication select
			reqParam.put("value(select" + (i + 1) + ")", "TI"); // may be "标题"
																// select
			reqParam.put("value(hidInput" + (i + 1) + ")", "");
		}

		// reqParam.put("x","77");
		// reqParam.put("y","18");

		reqParam.put("limitStatus", "expanded");
		reqParam.put("ss_lemmatization", "On");
		reqParam.put("ss_spellchecking", "Suggest");
		reqParam.put("SinceLastVisit_UTC", "");
		reqParam.put("SinceLastVisit_DATE", "");
		// reqParam.put("range","ALL"); // ALL or 4week
		// ，在getRangeParameter或getYearParameter中指定
		// reqParam.put("period","Year Range"); // Year Range or Range+Selection
		// ， 在getRangeParameter或getYearParameter中指定
		// reqParam.put("startYear",startYear); // 在getYearParameter中指定
		// reqParam.put("endYear",endYear); // 在getYearParameter中指定
		reqParam.put("editions", "SCI");
		reqParam.put("ssStatus", "display:none");
		reqParam.put("ss_showsuggestions", "ON");
		reqParam.put("ss_numDefaultGeneralSearchFields", "1");
		reqParam.put("ss_query_language", "");
		reqParam.put("rs_sort_by", "PY.D;LD.D;SO.A;VL.D;PG.A;AU.A");

		// 尝试是否能直接发送refine分类， but failed
		// reqParam.put("refineSelection","DocumentType_ARTICLE");
		// reqParam.put("refineSelection","DocumentType_REVIEW");
		// reqParam.put("refineSelection","DocumentType_LETTER");

		return reqParam;

		/*
		 * email_capture里的代码，做参考 reqParam.put("SID",this.sessionID);
		 * reqParam.put("SinceLastVisit_UTC","");
		 * reqParam.put("SinceLastVisit_DATE","");
		 * reqParam.put("action","search"); reqParam.put("collapse_alt",
		 * "Collapse these settings"); reqParam.put("collapse_title",
		 * "Collapse these settings");
		 * reqParam.put("defaultCollapsedListStatus","display: none");
		 * reqParam.put("defaultEditionsStatus","display: block");
		 * reqParam.put("editions","SCI"); reqParam.put("endDate", sysEndDate);
		 * reqParam.put("endYear", year); reqParam.put("expand_alt",
		 * "Expand these settings"); reqParam.put("expand_title",
		 * "Expand these settings"); reqParam.put("extraCollapsedListStatus",
		 * "display: inline"); reqParam.put("extraEditionsStatus",
		 * "display: none"); reqParam.put("fieldCount", "3");
		 * reqParam.put("input_invalid_notice",
		 * "Search Error: Please enter a search term");
		 * reqParam.put("input_invalid_notice_limits",
		 * "<br/>Note: Fields displayed in scrolling boxes must be combined with at least one other search field"
		 * ); reqParam.put("limitStatus", "collapsed");
		 * reqParam.put("max_field_count", "25");
		 * reqParam.put("max_field_notice",
		 * "Notice: You cannot add another field"); reqParam.put("period",
		 * "Range Selection"); reqParam.put("product", "WOS");
		 * reqParam.put("range", "ALL"); reqParam.put("rsStatus",
		 * "display:none"); reqParam.put("rs_linksWindows", "newWindow");
		 * reqParam.put("rs_rec_per_page", "10"); reqParam.put("rs_refinePanel",
		 * "visibility:show"); reqParam.put("rs_sort_by",
		 * "PY.D;LD.D;SO.A;VL.D;PG.A;AU.A"); reqParam.put("sa_img_alt",
		 * "Select terms from the index"); reqParam.put("sa_params",
		 * "WOS|http://0-apps.webofknowledge.com.brum.beds.ac.uk/InboundService.do%3Fproduct%3DWOS%26search_mode%3DGeneralSearch%26mode%3DGeneralSearch%26action%3Dtransfer%26viewType%3Dinput%26SID%3D"
		 * +this.sessionID+"%26inputbox%3Dinput???|"+this.sessionID+
		 * "|http://apps.webofknowledge.com|[name=au;value=au;keyname=;type=termlist;priority=10, name=GP;value=GP;keyName=;type=termlist;priority=10, name=SO;value=SO;keyName=;type=termlist;priority=10, name=OG;value=OG;keyName=;type=searchAid;priority=10]'"
		 * ); reqParam.put("search_mode", "GeneralSearch");
		 * reqParam.put("ss_lemmatization", "On");
		 * reqParam.put("ss_query_language", ""); reqParam.put("startDate",
		 * "2011-01-01"); reqParam.put("startYear", "1975");
		 * reqParam.put("timeIndex", "LoadDate");
		 * reqParam.put("timeSpanCollapsedListStatus", "display: none");
		 * reqParam.put("timespanStatus", "display: block");
		 * reqParam.put("value(bool_1_2)", input.getBool12());
		 * reqParam.put("value(bool_2_3)", input.getBool23());
		 * reqParam.put("value(hidInput1)", "");
		 * reqParam.put("value(hidInput2)", "AU");
		 * reqParam.put("value(hidInput3)", "SO");
		 * reqParam.put("value(hidShowIcon1)", "0");
		 * reqParam.put("value(hidShowIcon2)", "1");
		 * reqParam.put("value(hidShowIcon3)", "1");
		 * reqParam.put("value(input1)", input.getKeyword1());
		 * reqParam.put("value(input2)", input.getKeyword2());
		 * reqParam.put("value(input3)", input.getKeyword3());
		 * reqParam.put("value(select1)", input.getSearchRange1());
		 * reqParam.put("value(select2)", input.getSearchRange2());
		 * reqParam.put("value(select3)", input.getSearchRange3());
		 * reqParam.put("x", "43"); reqParam.put("y", "16");
		 * 
		 */

		/*
		 * SEARCH BODY fieldCount=1& action=search& product=WOS&
		 * search_mode=GeneralSearch& SID=4Ff3v7CxaSyzVBaFefX&
		 * max_field_count=25&
		 * max_field_notice=%E6%B3%A8%E6%84%8F%3A%20%E6%97%A0%E6%B3%95%E6%B7%BB%
		 * E5%8A%A0%E5%8F%A6%E4%B8%80%E5%AD%97%E6%AE%B5%E3%80%82&
		 * input_invalid_notice=%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A%20%E8%AF
		 * %B7%E8%BE%93%E5%85%A5%E6%A3%80%E7%B4%A2%E8%AF%8D%E3%80%82&
		 * exp_notice=%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A%20%E4%B8%93%E5%88%
		 * A9%E6%A3%80%E7%B4%A2%E8%AF%8D%E5%8F%AF%E5%9C%A8%E5%A4%9A%E4%B8%AA%E5%
		 * AE%B6%E6%97%8F%E4%B8%AD%E6%89%BE%E5%88%B0%20(&
		 * input_invalid_notice_limits=%20%3Cbr%2F%3E%E6%B3%A8%3A%20%E6%BB%9A%E5
		 * %8A%A8%E6%A1%86%E4%B8%AD%E6%98%BE%E7%A4%BA%E7%9A%84%E5%AD%97%E6%AE%B5
		 * %E5%BF%85%E9%A1%BB%E8%87%B3%E5%B0%91%E4%B8%8E%E4%B8%80%E4%B8%AA%E5%85
		 * %B6%E4%BB%96%E6%A3%80%E7%B4%A2%E5%AD%97%E6%AE%B5%E7%9B%B8%E7%BB%84%E9
		 * %85%8D%E3%80%82&
		 * sa_params=WOS%7C%7C4Ff3v7CxaSyzVBaFefX%7Chttp%3A%2F%2Flib-proxy.pnc.
		 * edu%3A2311%7C'& value(input1)=cell%20stem%20cell& value(select1)=SO&
		 * value(hidInput1)=& limitStatus=expanded& ss_lemmatization=On&
		 * ss_spellchecking=Suggest& SinceLastVisit_UTC=& SinceLastVisit_DATE=&
		 * range=ALL& period=Year%20Range& startYear=2009& endYear=2014&
		 * editions=SCI& ssStatus=display%3Anone& ss_showsuggestions=ON&
		 * ss_query_language=&
		 */
	}

	private static Map<String, String> getRefineParams(String sid, String parentQid) {
		Map<String, String> reqParam = new IdentityHashMap<String, String>();
		reqParam.put("parentQid", parentQid);
		reqParam.put("SID", sid);
		reqParam.put("product", "WOS");
		reqParam.put("databaseId", "WOS");
		reqParam.put("colName", "WOS");
		reqParam.put("search_mode", "Refine");
		reqParam.put("queryOption(summary_search_mode)", "GeneralSearch");
		reqParam.put("action", "search");
		reqParam.put("clickRaMore", "如果继续使用\"更多...\"功能，则屏幕中的精炼选择将不会保存。");
		reqParam.put("openCheckboxes", "如果隐藏左侧面板，则其中的精炼选择将不会保存。");
		reqParam.put("refineSelectAtLeastOneCheckbox", "请至少选中一个复选框来精炼检索结果。");
		reqParam.put("queryOption(sortBy)", "PY.D;LD.D;SO.A;VL.D;PG.A;AU.A");
		reqParam.put("queryOption(ss_query_language)", "auto");
		reqParam.put("sws", "");
		reqParam.put("defaultsws", "在如下结果集内检索...");
		reqParam.put("swsFields", "TS");
		reqParam.put("swsHidden", "在前 100,000 条结果内<br>检索");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put(new String("refineSelection"), "DocumentType_ARTICLE"); // IdentityHashMap判断key1==key2?，所以要写成两个不同对象形式
		reqParam.put(new String("refineSelection"), "DocumentType_REVIEW");
		reqParam.put(new String("refineSelection"), "DocumentType_LETTER");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("exclude", "");
		reqParam.put("mode", "refine");

		return reqParam;

		/*
		 * parentQid=4& SID=4Ff3v7CxaSyzVBaFefX& product=WOS& databaseId=WOS&
		 * colName=WOS& search_mode=Refine&
		 * queryOption%28summary_search_mode%29=GeneralSearch& action=search&
		 * clickRaMore=%E5%A6%82%E6%9E%9C%E7%BB%A7%E7%BB%AD%E4%BD%BF%E7%94%A8%22
		 * %E6%9B%B4%E5%A4%9A...%22%E5%8A%9F%E8%83%BD%EF%BC%8C%E5%88%99%E5%B1%8F
		 * %E5%B9%95%E4%B8%AD%E7%9A%84%E7%B2%BE%E7%82%BC%E9%80%89%E6%8B%A9%E5%B0
		 * %86%E4%B8%8D%E4%BC%9A%E4%BF%9D%E5%AD%98%E3%80%82&
		 * openCheckboxes=%E5%A6%82%E6%9E%9C%E9%9A%90%E8%97%8F%E5%B7%A6%E4%BE%A7
		 * %E9%9D%A2%E6%9D%BF%EF%BC%8C%E5%88%99%E5%85%B6%E4%B8%AD%E7%9A%84%E7%B2
		 * %BE%E7%82%BC%E9%80%89%E6%8B%A9%E5%B0%86%E4%B8%8D%E4%BC%9A%E4%BF%9D%E5
		 * %AD%98%E3%80%82&
		 * refineSelectAtLeastOneCheckbox=%E8%AF%B7%E8%87%B3%E5%B0%91%E9%80%89%
		 * E4%B8%AD%E4%B8%80%E4%B8%AA%E5%A4%8D%E9%80%89%E6%A1%86%E6%9D%A5%E7%B2%
		 * BE%E7%82%BC%E6%A3%80%E7%B4%A2%E7%BB%93%E6%9E%9C%E3%80%82&
		 * queryOption%28sortBy%29=PY.D%3BLD.D%3BSO.A%3BVL.D%3BPG.A%3BAU.A&
		 * queryOption%28ss_query_language%29=auto& sws=&
		 * defaultsws=%E5%9C%A8%E5%A6%82%E4%B8%8B%E7%BB%93%E6%9E%9C%E9%9B%86%E5%
		 * 86%85%E6%A3%80%E7%B4%A2...& swsFields=TS&
		 * swsHidden=%E5%9C%A8%E5%89%8D+100%2C000+%E6%9D%A1%E7%BB%93%E6%9E%9C%E5
		 * %86%85%3Cbr%3E%E6%A3%80%E7%B4%A2& exclude=& exclude=&
		 * refineSelection=DocumentType_ARTICLE&
		 * refineSelection=DocumentType_REVIEW&
		 * refineSelection=DocumentType_LETTER& exclude=& exclude=& exclude=&
		 * exclude=& exclude=& exclude=& exclude=& exclude=& exclude=& exclude=&
		 * exclude=& exclude=& exclude=& mode=refine
		 */
	}

	/**
	 * 对title中的()和特殊字符处理 ()和[]中的内容去掉，非数字和英文的特殊字符都替换为空格
	 * 
	 * @param str
	 * @return
	 */
	private String charaterDeal(String str) {
		str = str.replaceAll("pos&lt;|sup&gt;|sub&gt;|i&gt;|t&lt;|&lt;", "").replaceAll("\\([^)]*\\)", "")
				.replaceAll("\\[[^]]*\\]", "").replaceAll("[^\\w]", " ").replaceAll("\\s+", " ");
		str = str.replace("hellip", "");
		return str.trim();
	}

	/**
	 * 通过WOS文章标题，找到对应BrandTitleTemp，给文章加上googlescholar关键词属性wos.setSearchKeyword(
	 * )
	 * 
	 * @return 返回与之标题相同的BrandTitleTemp
	 */
	private BrandTitleTemp findInTitles(WOS wos, List<BrandTitleTemp> brandTitleTemps) {
		for (BrandTitleTemp btt : brandTitleTemps) {
			String wosTemp = wos.getTitle().replaceAll("[^\\w]", " ").replaceAll("\\s+", " ");
			if ((wosTemp.split(" ").length - 3) > btt.getOriginalTitle().split(" ").length) { // 说明WOS里搜到的标题比实际搜索的标题长很多，不是同一篇文章
				continue;
			}
			if (contains(wosTemp, charaterDeal(btt.getOriginalTitle()))) {
				wos.setSearchKeyword(btt.getKeyword());
				return btt;
			}
		}
		return null;
	}

	/**
	 * 判断str字符串是否包含substr的每一个单词（按空格分）
	 * 
	 * @param str
	 * @param substr
	 * @return
	 */
	private boolean contains(String str, String substr) {
		String[] subs = substr.split(" ");
		Pattern pattern;
		for (String sub : subs) {
			pattern = Pattern.compile("\\b" + sub + "\\b", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(str);
			if (!matcher.find()) {
				return false;
			}
		}
		return true;
	}

	private Map<String, String> wosLogin(String account) {
		
		for (int i = 0; i < 10; i++) {
			Map<String, String> loginMap = this.loginAccount(account);
			if (loginMap == null) {
				System.out.println("登陆失败");
				this.getLoggerTA().append("登陆失败\n");
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			} else {
				System.out.println("WOS登陆成功");
				this.getLoggerTA().append("WOS登陆成功\n");
				return loginMap;
			}
		}
		return null;
	}

	public boolean isStartFlag() {
		return startFlag;
	}

	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}

	public JTextArea4Log getLoggerTA() {
		return loggerTA;
	}

}
