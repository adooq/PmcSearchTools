package selleck.email.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import selleck.email.pojo.LabmemReference;
import selleck.email.service.IEdmEmailGeneralService;
import selleck.email.service.impl.EdmEmailGeneralServiceImpl;

public class EmailPickByJinxin {	
	private IEdmEmailGeneralService mapper = new EdmEmailGeneralServiceImpl();
	private List<String> allUrlList = new ArrayList<String>();
	private boolean error = true;	//false表示url读取错误判断
	
	/*
	 * 抓取当前页面email
	 * 
	 */
	public List<String> catchEamilList(String url) {
		System.out.println("catchEamilList url:  "+url);
		List<String> emailList = new ArrayList<String>();
		String regex = "[\\w[.-]]+@[\\w[.-]]+\\.[\\w]+";
		Pattern p = Pattern.compile(regex);
		Parser parser;
		try {
			parser = new Parser(url);
			NodeFilter filter = new RegexFilter(regex);
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
			if (nodes.size() > 0) {
				for (NodeIterator ni = nodes.elements(); ni.hasMoreNodes();) {
					Matcher m = p.matcher(ni.nextNode().toHtml());
					if (m.find()) {
						emailList.add(m.group());
						System.out.println("email found: "+m.group());
					}
				}
			}
		} catch (ParserException e) {
			e.printStackTrace();
			error = false;
		}

		return emailList;
	}
	
	/*
	 * 抓取当前页面email
	 * 
	 */
	/* @deprecated
	public List<String> catchEamilList(String url){
		URL url1 = null;  
		List<String> emailList = new ArrayList<String>();
		String regex = "[\\w[.-]]+@[\\w[.-]]+\\.[\\w]+";
		String tempStr = null;
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = null;  
		try {  			   
			url1 = new URL(url);   
			HttpURLConnection urlConnection = (HttpURLConnection)url1.openConnection();   
			urlConnection.connect();  
			urlConnection.setConnectTimeout(30000000);//连接超时
			urlConnection.setReadTimeout(30000000);//读操作超时
			InputStream in = urlConnection.getInputStream();     
			byte[] buf = new byte[4096];              
			while (in.read(buf) > 0) {           
				tempStr = new String(buf);  
				matcher = pattern.matcher(tempStr);  
				while(matcher.find()) {  
					if(emailList.size()==0){
						emailList.add(matcher.group());						
					}else{
						boolean flag = true;
						for(String email : emailList){
							if(matcher.group().equals(email)){
								flag = false;
							}
						}
						if(flag){
							emailList.add(matcher.group());
						}
					}
				}  
			}  
			urlConnection.disconnect();
		}catch (IOException e) {  
			//e.printStackTrace();
			error = false;
		}  
		return emailList;
	}
	*/
	
	/*
	 * 抓取当前页面所有link链接
	 * 
	 */
	public List<String> catchUrlList(String url){ 
		List<String> urlList = new ArrayList<String>();
		Parser  parser = null;
		NodeList nodeList = null;
		try {
			parser = new Parser(url);
			NodeFilter filter = new TagNameFilter("A");
			nodeList = parser.extractAllNodesThatMatch(filter);
		} catch (ParserException e) {
			e.printStackTrace();
		}
		
		 for (int i=0; i<nodeList.size(); i++) {
	           LinkTag tag = (LinkTag) nodeList.elementAt(i);
	           String link = tag.getLink();
	           // System.out.print(tag.getLinkText() + "-->");
	           if(!link.contains("@")){
	        	   System.out.println("link: "+tag.getLink());
		           urlList.add(tag.getLink());
	           }
	           // System.out.println(tag.getAttribute("href"));
	       }
		
		return urlList;
	}
	
	/*
	 * 抓取当前页面所有link链接
	 * 
	 */
	/* @deprecated
	public List<String> catchUrlList(String url){
		URL url1 = null;  
		List<String> urlList = new ArrayList<String>();
		String regex ="<(a).[^>]*(href)=(\"|'|)(.[^\"|'||\\s]*)(\")[^>]*>"; 
		String tempStr = null;
		Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE); 
		Matcher matcher = null;  
		try {  
			url1 = new URL(url);   
			HttpURLConnection urlConnection = (HttpURLConnection)url1.openConnection();   
			urlConnection.connect();  
			// urlConnection.setConnectTimeout(30000000);//连接超时
			// urlConnection.setReadTimeout(30000000);//读操作超时
			InputStream in = urlConnection.getInputStream();     
			byte[] buf = new byte[4096];              
			while (in.read(buf) > 0) {           
				tempStr = new String(buf);  
				matcher = pattern.matcher(tempStr);  
				while(matcher.find()) {  
					urlList.add(matcher.group());						
				}  
			}  
			urlConnection.disconnect();
		}catch (IOException e) {  
			//e.printStackTrace();  
			error = false;
		}
		
		return urlList;
	}
	*/
	
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
		currentEmailList(url,organitName,email);
		List<String> aa = new ArrayList<String>();
		//抓取当前页面link链接
		List<String> urlList =catchUrlList(url);
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
		//获取次级url路径
		List<String> urlList2 = backEmailList(url, urlList);
		
		LabmemReference labmem = new LabmemReference();
		if(urlList2.size()!=0){
			for(String catchUrl : urlList2){
				email.setOrganitURL(catchUrl);
				allEmailList(catchUrl,organitName,labmem);									
			}
		}
	}
	
	/**
	 * 精确URL 抓取一层所有EAMIL
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
		currentEmailList(url,name,labmen);
		//抓取当前页面link链接
		List<String> urlList =catchUrlList(url);
		//获取次级url路径
		// List<String> urlList2 = backEmailList(url, urlList);
		
//		if(urlList2.size()!=0){
//			for(String catchUrl : urlList2){				
//				currentEmailList(catchUrl,name,labmen);							
//			}
//		}
		
		if(urlList.size()!=0){
			for(String catchUrl : urlList){				
				currentEmailList(catchUrl,name,labmen);							
			}
		}
	}
	
	/*
	 * 精确URL 抓取当前层所有EAMIL
	 */
	public void currentEmailList(String url,String name,LabmemReference labmen){
		//默认error
		error = true;
		//日期显示表示年月
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");   
		String dateString = format.format(new Date());
		Criteria criteria = new Criteria();
		EdmEmailGeneral edmEmail = new EdmEmailGeneral();
		//抓取当前页面所有email
		List<String> emailList = catchEamilList(url);
		//查询当前url所有email信息
		criteria.put("url", url);
		List<EdmEmailGeneral> edmEmailList = mapper.selectByExample(criteria);
		//flag:true 当前eamil和已存email不相同,反之,则不做添加处理
		boolean flag = true;
		if(!error){
			//"*"表示当前url错误或者读取超时
			for(EdmEmailGeneral mail : edmEmailList){
				if("*".equals(mail.getEmail()) &&url.equals(mail.getUrl())){
					flag = false;
					break;
				}
			}			
			if(flag){
				edmEmail.setDataId(labmen.getDataId());
				//edmEmail.setDataId(2);
				edmEmail.setAddress(labmen.getAddress());
				edmEmail.setUrl(url);
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
			return;
		}
		
		//更新eamilList,用作不相同email的保存
		List<String> newEmailList = new ArrayList<String>();
		if(edmEmailList.size() != 0){
			//保存当前页面email信息
			for(String email1 : emailList){
				//flag:true 当前eamil和已存email不相同,反之,则不做添加处理
				flag = true;
				for(EdmEmailGeneral mail : edmEmailList){
					if(email1.equals(mail.getEmail())){
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
				for(String email2 : emailList){
					if(mail1.getEmail().equals(email2)){
						updateFlag = true;
						break;
					}
				}
				if(!updateFlag){
					//updateFlag 1:表示更新操作中这条记录已经不存在
					mail1.setUpdateFlag(1);
					mapper.updateByPrimaryKey(mail1);
				}			
			}
			//更新新的email信息
			for(String addNewEmail:newEmailList){
				edmEmail.setDataId(labmen.getDataId());
				edmEmail.setAddress(labmen.getAddress());
				edmEmail.setUrl(url);
				edmEmail.setName(name);
				edmEmail.setEmail(addNewEmail);
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
			for(String addEmail : emailList){
				if(!addEmail.equals(labmen.getBossEmail())){
					edmEmail.setDataId(labmen.getDataId());
					//edmEmail.setDataId(1);
					edmEmail.setAddress(labmen.getAddress());
					edmEmail.setUrl(url);
					edmEmail.setName(name);
					edmEmail.setEmail(addEmail);
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
