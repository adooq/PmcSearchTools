package selleck.email.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import common.handle.model.Criteria;
import selleck.email.pojo.EdmEmailGeneral;
import selleck.email.pojo.Email;
import selleck.email.pojo.LabmemReference;
import selleck.email.service.IEdmEmailGeneralService;
import selleck.email.service.impl.EdmEmailGeneralServiceImpl;

public class EmailPick {	
	private IEdmEmailGeneralService mapper = new EdmEmailGeneralServiceImpl();
	private List<String> allUrlList = new ArrayList<String>();
	private boolean error = true;	//false表示url读取错误判断
	
	/**
	 * 抓取当前页面email
	 * @param realURL
	 * @param sourceURL
	 */
	public Set<Email> catchEamilList(String realURL , String sourceURL) {
		System.out.println("catchEamilList url:  "+realURL);
		Set<Email> emailList = new HashSet<Email>();
		if(realURL.trim().equals("")){
			return emailList;
		}
		String regex = "[\\w[.-]]+@[\\w[.-]]+\\.[\\w]+";
		Pattern p = Pattern.compile(regex);
		Parser parser;
		try {
			parser = new Parser(realURL);
			NodeFilter filter = new RegexFilter(regex);
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
			if (nodes.size() > 0) {
				for (NodeIterator ni = nodes.elements(); ni.hasMoreNodes();) {
					Matcher m = p.matcher(ni.nextNode().toHtml());
					if (m.find()) {
						Email email = new Email();
						email.setEmail(m.group());
						email.setRealURL(realURL);
						email.setSourceURL(sourceURL);
						emailList.add(email);
						System.out.println("email found: "+email.getEmail());
					}
				}
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}

		return emailList;
	}

	
	/*
	 * 抓取当前页面所有link链接
	 * 
	 */
	public Set<String> catchUrlList(String url) {
		Set<String> urlList = new HashSet<String>();
		Parser parser = null;
		NodeList nodeList = null;
		try {
			parser = new Parser(url);
			NodeFilter filter = new TagNameFilter("A");
			nodeList = parser.extractAllNodesThatMatch(filter);
			for (int i = 0; i < nodeList.size(); i++) {
				LinkTag tag = (LinkTag) nodeList.elementAt(i);
				String link = tag.getLink();
				// System.out.print(tag.getLinkText() + "-->");
				if (!link.contains("@") && !link.endsWith(".pdf") && !link.endsWith(".jpg") && !link.endsWith(".mov")) {
					System.out.println("link: " + tag.getLink());
					urlList.add(tag.getLink());
					// System.out.println(tag.getAttribute("href"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return urlList;
	}
	
	
	public List<String> backEmailList(String url,List<String> urlList){
		List<String> urlList2 = new ArrayList<String>();
		String http = "http://";
		String https = "https://";
		boolean httpFlag = false;
		int length = http.length();		
		if(url.contains(https)){
			httpFlag = true;
			length = https.length();
		}

		url = url.substring(length);
		int urlIndex = url.indexOf("/");
		//获取当前url主页面
		String urlStart = null;
		if(urlIndex == -1){
			urlStart = url;
		}else{
			urlStart = url.substring(0,urlIndex);	
		}
		//获取当前url次级链接
		int urlLastIndex = url.lastIndexOf("/");
		String urlLast = null;
		if(urlLastIndex != -1){
			urlLast = url.substring(0,urlLastIndex);
		}else{
			urlLast = url;
		}
		for(String catchUrl : urlList){
			boolean flag = true;
			//获取link链接
			// 以下代码有很多漏洞，href= 中间可能有空格，不一定有引号，html注释，#，javascript，页面编码等等，需要好好重写
			// 抓到的url可能有几种情况，http开头、/开头、普通字符(相对地址)，但要去除不需要的 mailto:、#
			int index = catchUrl.indexOf("href=");
			catchUrl = catchUrl.substring(index+6);
			int endIndex = catchUrl.indexOf("\"");
			
			catchUrl = catchUrl.substring(0,endIndex);
			System.out.println("catchUrl   "+ catchUrl);
			String x = catchUrl.substring(0, 1);
			// System.out.println(catchUrl);
			//判断是否符合链接要求
			if(x.equals("h")){
				if(catchUrl.contains(url) && catchUrl.equals(url)){
					if(urlList2.size()==0){
						if(httpFlag){
							catchUrl = https + catchUrl;
						}else{
							catchUrl = http + catchUrl;							
						}
						urlList2.add(catchUrl);					
					}else{
						for(String hostUrl : urlList2){
							if(hostUrl.equals(catchUrl)){
								flag = false;
							}
						}
						if(flag){
							if(httpFlag){
								catchUrl = https + catchUrl;
							}else{
								catchUrl = http + catchUrl;							
							}
						}
						urlList2.add(catchUrl);		
					}					
				}
			}else if(x.equals("/")){
				String buffUrl = urlStart + catchUrl;
				if(buffUrl.contains(url) && !buffUrl.equals(url)){
					if(urlList2.size()==0){
						if(httpFlag){
							buffUrl = https + buffUrl;
						}else{
							buffUrl = http + buffUrl;							
						}
						urlList2.add(buffUrl);
					}else{
						for(String hostUrl2 : urlList2){
							if(hostUrl2.equals(buffUrl)){
								flag = false;
							}
						}
						if(flag){
							if(httpFlag){
								buffUrl = https + buffUrl;
							}else{
								buffUrl = http + buffUrl;							
							}
						}
						urlList2.add(buffUrl);
					}
				}
			}else if(catchUrl.contains("index")){
				if(url.contains("index")){
					int inx = url.indexOf("index");
					url = url.substring(0,inx-1);
				}
				String newUrl = url + "/" +catchUrl;
				if(urlList2.size()==0){
					if(httpFlag){
						newUrl = https + newUrl;
					}else{
						newUrl = http + newUrl;							
					}
					urlList2.add(newUrl);
				}else{
					for(String hostUrl3 : urlList2){
						if(hostUrl3.equals(newUrl)){
							flag = false;
						}
					}
					if(flag){
						if(httpFlag){
							newUrl = https + newUrl;
						}else{
							newUrl = http + newUrl;							
						}
					}
					urlList2.add(newUrl);
				}
				
			}			
				
		}
		return urlList2;
	}
	
	/*
	 * 抓取所有EAMIL
	 */
	public void allEmailList(String url,String organitName,LabmemReference email){
		
		//抓取当前页面所有email
		// currentEmailList(url,organitName,email);
		List<String> aa = new ArrayList<String>();
		//抓取当前页面link链接
		Set<String> urlList = catchUrlList(url);
		if(allUrlList.size()==0){
			allUrlList.addAll(urlList);			
		}else{
			for(String a : urlList){
				for(String a1 : allUrlList){
					if(a.equals(a1)){
						aa.add(a);	
						break;
					}
				}
			}
			if(aa.size()!=0)
				urlList.removeAll(aa);
			if(urlList.size()!=0){
				allUrlList.addAll(urlList);
			}
		}
		if(urlList.size()==0){
			return;
		}
//		//获取次级url路径
//		List<String> urlList2 = backEmailList(url, urlList);
//		
//		LabmemReference labmem = new LabmemReference();
//		if(urlList2.size()!=0){
//			for(String catchUrl : urlList2){
//				email.setOrganitURL(catchUrl);
//				allEmailList(catchUrl,organitName,labmem);									
//			}
//		}
	}
	
	/**
	 * 抓取当前url和次级url的EAMIL
	 * @param url   laburl
	 * @param name   boss name
	 * @param labmen
	 */
	public void exactEmailList(String url,String name,LabmemReference labmen){
		
		if(url.equals("")){
			return;
		}
		//没有BOSSUrl的时候 name取FACULTY
		if(labmen.getBossURL() == null){
			name = "FACULTY";
		}
		
		//抓取当前页面所有email
		Set<Email> emailList = catchEamilList(url,url);
		
		//抓取当前页面link链接
		Set<String> urlList = catchUrlList(url);
		
		//抓取次级页面的email
		if(urlList.size()!=0){
			for(String catchUrl : urlList){				
				emailList.addAll(catchEamilList(catchUrl,url));							
			}
		}
		
		saveEmailList(emailList,url,name,labmen);
	}
	
	/**
	 * 保存搜索到的EAMIL
	 * @param emailList  
	 * @param sourceURL  来源实验室、机构、BOSS的url
	 * @param name  
	 * @param labmen
	 */
	public void saveEmailList(Set<Email> emailList,String sourceURL,String name,LabmemReference labmen){
		//日期显示表示年月
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");   
		String dateString = format.format(new Date());
		Criteria criteria = new Criteria();
		EdmEmailGeneral edmEmail = new EdmEmailGeneral();
		
		// 记录没有搜到email的laburl，可能后期人工检查
		if(emailList == null || emailList.isEmpty()){
			edmEmail.setDataId(labmen.getDataId());
			edmEmail.setAddress(labmen.getAddress());
			edmEmail.setUrl(sourceURL);
			edmEmail.setRealURL("*");
			edmEmail.setName(name);
			edmEmail.setEmail("*");
			edmEmail.setUniversity(labmen.getUniversity());
			edmEmail.setCountry(labmen.getCountry());
			edmEmail.setResearchFiled(labmen.getResearchFiled());
			edmEmail.setInterest(labmen.getInterest());
			edmEmail.setDetail(labmen.getDetail());
			edmEmail.setNote(labmen.getNote());
			edmEmail.setCity(labmen.getCity());
			edmEmail.setPhone(labmen.getPhone());
			edmEmail.setZipCode(labmen.getZipCode());
			edmEmail.setFinder(labmen.getFinder());
			edmEmail.setChecker(labmen.getChecker());
			edmEmail.setPickDate(labmen.getPickDate());
			edmEmail.setReadFlag(labmen.getReadFlag());
			edmEmail.setCreateDate(dateString);
			edmEmail.setUpdateDate(dateString);
			edmEmail.setUpdateFlag(0);
			mapper.insert(edmEmail);
		}
		
		emailList.add(new Email(labmen.getBossEmail(),"*",labmen.getLabURL())); // 把原本就有的boss的email也加入
		
		criteria.put("url", sourceURL);
		List<EdmEmailGeneral> edmEmailList = mapper.selectByExample(criteria);
		boolean flag = true;
		//更新eamilList,用作不相同email的保存
		List<Email> newEmailList = new ArrayList<Email>();
		if(edmEmailList.size() != 0){
			//保存当前页面email信息
			for(Email email1 : emailList){
				//flag:true 当前eamil和已存email不相同,反之,则不做添加处理
				flag = true;
				for(EdmEmailGeneral mail : edmEmailList){
					if(email1.getEmail().equals(mail.getEmail())){
						flag = false;
						break;
					}
				}
				
				if(flag){
					newEmailList.add(email1);
				}
			}
						
			//拿过去的eamil和当前email比较,更新已经不存在的eamil信息
			for(EdmEmailGeneral mail1 : edmEmailList){
				boolean updateFlag = false;
				for(Email email2 : emailList){
					if(mail1.getEmail().equals(email2.getEmail())){
						updateFlag = true;
						break;
					}
				}
				if(!updateFlag){
					//updateFlag 1:表示更新操作中这条记录已经不存在
					mail1.setUpdateFlag(1);
					mail1.setUpdateDate(dateString);
					mapper.updateByPrimaryKey(mail1);
				}			
			}
			//更新新的email信息
			for(Email addNewEmail:newEmailList){
				edmEmail.setDataId(labmen.getDataId());
				edmEmail.setAddress(labmen.getAddress());
				edmEmail.setUrl(sourceURL);
				edmEmail.setRealURL(addNewEmail.getRealURL());
				edmEmail.setName(name);
				edmEmail.setEmail(addNewEmail.getEmail());
				edmEmail.setUniversity(labmen.getUniversity());
				edmEmail.setCountry(labmen.getCountry());
				edmEmail.setResearchFiled(labmen.getResearchFiled());
				edmEmail.setInterest(labmen.getInterest());
				edmEmail.setDetail(labmen.getDetail());
				edmEmail.setNote(labmen.getNote());
				edmEmail.setCity(labmen.getCity());
				edmEmail.setPhone(labmen.getPhone());
				edmEmail.setZipCode(labmen.getZipCode());
				edmEmail.setFinder(labmen.getFinder());
				edmEmail.setChecker(labmen.getChecker());
				edmEmail.setPickDate(labmen.getPickDate());
				edmEmail.setReadFlag(labmen.getReadFlag());
				edmEmail.setCreateDate(dateString);
				edmEmail.setUpdateDate(dateString);
				//更新email时updateFlag为2
				edmEmail.setUpdateFlag(2);
				mapper.insert(edmEmail);								
			}
		}else{
			for(Email addEmail : emailList){
				if(!addEmail.equals(labmen.getBossEmail())){
					edmEmail.setDataId(labmen.getDataId());
					//edmEmail.setDataId(1);
					edmEmail.setAddress(labmen.getAddress());
					edmEmail.setUrl(sourceURL);
					edmEmail.setRealURL(addEmail.getRealURL());
					edmEmail.setName(name);
					edmEmail.setEmail(addEmail.getEmail());
					edmEmail.setUniversity(labmen.getUniversity());
					edmEmail.setCountry(labmen.getCountry());
					edmEmail.setResearchFiled(labmen.getResearchFiled());
					edmEmail.setInterest(labmen.getInterest());
					edmEmail.setDetail(labmen.getDetail());
					edmEmail.setNote(labmen.getNote());
					edmEmail.setCity(labmen.getCity());
					edmEmail.setPhone(labmen.getPhone());
					edmEmail.setZipCode(labmen.getZipCode());
					edmEmail.setFinder(labmen.getFinder());
					edmEmail.setChecker(labmen.getChecker());
					edmEmail.setPickDate(labmen.getPickDate());
					edmEmail.setReadFlag(labmen.getReadFlag());
					edmEmail.setCreateDate(dateString);
					edmEmail.setUpdateDate(dateString);
					edmEmail.setUpdateFlag(0);
					mapper.insert(edmEmail);					
				}
			}			
		}
	}
	
	/**
	 * 抓取bossEmail信息
	 * @param labmen
	 */
	public void catchBossEmail(LabmemReference labmen){
		Criteria criteria = new Criteria();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");   
		String dateString = format.format(new Date());
		EdmEmailGeneral edmEmail = new EdmEmailGeneral();
		//flag:true 当前eamil和已存email不相同,反之,则不做添加处理
		boolean flag = true;
		//查询当前url所有email信息
		criteria.put("url", labmen.getBossURL());
		List<EdmEmailGeneral> edmEmailList = mapper.selectByExample(criteria);
		if(edmEmailList.size() != 0){			
			//保存当前页面email信息
			for(EdmEmailGeneral mail : edmEmailList){
				if(labmen.getBossEmail().equals(mail.getEmail()) &&
						labmen.getBossURL().equals(mail.getUrl())){
					flag = false;
					break;
				}
			}
		}
		if(flag){
			edmEmail.setDataId(labmen.getDataId());
			//edmEmail.setDataId(1);
			edmEmail.setAddress(labmen.getAddress());
			edmEmail.setUrl(labmen.getBossURL());
			edmEmail.setName(labmen.getBossName());
			edmEmail.setEmail(labmen.getBossEmail());
			edmEmail.setUniversity(labmen.getUniversity());
			edmEmail.setCountry(labmen.getCountry());
			edmEmail.setResearchFiled(labmen.getResearchFiled());
			edmEmail.setInterest(labmen.getInterest());
			edmEmail.setDetail(labmen.getDetail());
			edmEmail.setNote(labmen.getNote());
			edmEmail.setCity(labmen.getCity());
			edmEmail.setPhone(labmen.getPhone());
			edmEmail.setZipCode(labmen.getZipCode());
			edmEmail.setFinder(labmen.getFinder());
			edmEmail.setChecker(labmen.getChecker());
			edmEmail.setPickDate(labmen.getPickDate());
			edmEmail.setReadFlag(labmen.getReadFlag());
			edmEmail.setCreateDate(dateString);
			edmEmail.setUpdateDate(dateString);
			edmEmail.setUpdateFlag(0);
			mapper.insert(edmEmail);	
		}		
	}		
}
