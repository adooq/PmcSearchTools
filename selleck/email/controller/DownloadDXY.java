package selleck.email.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.DXY_Reply;
import selleck.email.pojo.DXY_Topic;
import selleck.email.service.IDXYService;
import selleck.email.service.impl.DXYServiceImpl;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.Constants;
import selleck.utils.HTTPUtils;

public class DownloadDXY {
	/* 设置要修改的参数 */
	public static final int FORUM_ID = 41; // 版块id
	public static final int MAX_PAGE = 831; // 最大页数
	

	/** 下载丁香园的帖子
	 * @param args
	 */
	public static void main(String[] args) {
		IDXYService dxyService = new DXYServiceImpl(Constants.LOCAL);
		
		Map<String,String> htmlMap = null;
		List<String> lists= null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		for(int page = 1; page <= MAX_PAGE;page++){
		// for(int page = 1; page <= 1095;page++){
			htmlMap = HTTPUtils.getCookieUrlAndHtml("http://www.dxy.cn/bbs/board/"+FORUM_ID+"?order=2&tpg="+page, null , null, HTTPUtils.GET, null);
			if(htmlMap.get("html") == null){
				System.out.println("翻页失败");
				try {
					Thread.sleep(1800000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				page --;
				continue;
			}
			
			lists = ParserUtils.findWithPrefixAndSuffix("<tr class=\"((odd)|(even)) hoverClass ( end)?\">", "</tr>", htmlMap.get("html"));
			if(lists.size() == 0){
				System.out.println("翻页失败");
				try {
					Thread.sleep(1800000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				page --;
				continue;
			}
			
			for(String titleTR : lists){
				// 帖子标题 <td class="news">
				List<String> titles = ParserUtils.findWithPrefixAndSuffix("<td class=\"news\">", "</td>", titleTR);
				if(titles.size() > 0){
					DXY_Topic dxyTopic = new DXY_Topic();
					
					String title = titles.get(0).substring(0, titles.get(0).indexOf("</a>"));
					title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
					title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
					title = title.trim();
					dxyTopic.setTitle(title);
					
					// 开帖日期
					List<String>  founds = ParserUtils.findInContent("\\d{4}\\-\\d{2}\\-\\d{2}", titleTR);
					if(founds.size() > 0){
						try {
							Date date = sdf.parse(founds.get(0));
							dxyTopic.setOpenDate(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					
					// 帖子url
					founds = ParserUtils.findInContent("http://[^\"]+", titles.get(0));
					if(founds.size() > 0){
						dxyTopic.setUrl(founds.get(0));
					}
					
					// 查看最多有几页，供后面帖子里翻页用
					int maxPage = 1;
					if(titles.get(0).contains("<span class=\"pag\">")){
						List<String> pageUrls = ParserUtils.findInContent(dxyTopic.getUrl()+"\\?ppg=\\d+", titles.get(0));
						// 默认最后一个url就是最大的页数
						String maxPageUrl = pageUrls.get(pageUrls.size()-1);
						maxPage = Integer.valueOf(maxPageUrl.substring(maxPageUrl.lastIndexOf("=")+1));
					}
					
					dxyTopic.setForumId(FORUM_ID);
					dxyService.saveTopic(dxyTopic);
					
					// id == 0 ，说明已经有该帖子，所以也不用点进去抓帖子回复
					if(dxyTopic.getId() == 0){
						continue;
					}
					
					// 看看帖子标题是否包含求助，是求助的就去抓帖子回复 , 【求助】
					if(!title.startsWith("【求助】")){
						continue;
					}
					
					if(dxyTopic.getUrl() != null && !dxyTopic.getUrl().isEmpty()){
						htmlMap = HTTPUtils.getCookieUrlAndHtml(dxyTopic.getUrl(), null, null, HTTPUtils.GET, null);
						if(htmlMap.get("html") != null){
							// <td class="postbody">
							List<String> replys = ParserUtils.findWithPrefixAndSuffix("<td class=\"postbody\">", "</td>", htmlMap.get("html"));
							for(String reply : replys){
								DXY_Reply dr = new DXY_Reply();
								reply = Jsoup.parse(reply).text(); // 去html标签 ,去换行\t\n\x0B\f\r
								reply = StringEscapeUtils.unescapeHtml4(reply); // 去转义字符，  &gt;  转换成>符号
								reply = reply.trim();
								dr.setContent(reply);
								dr.setTopicId(dxyTopic.getId());
								dxyService.saveReply(dr);
							}
						}
						
						// 查看后面的页数
						for(int p = 2 ; p<= maxPage;p++){
							htmlMap = HTTPUtils.getCookieUrlAndHtml(dxyTopic.getUrl()+"?ppg="+p, null, null, HTTPUtils.GET, null);
							if(htmlMap.get("html") != null){
								// <td class="postbody">
								List<String> replys = ParserUtils.findWithPrefixAndSuffix("<td class=\"postbody\">", "</td>", htmlMap.get("html"));
								if(replys.size() == 0){
									System.out.println("帖子翻页失败");
									try {
										Thread.sleep(1800000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									p --;
									continue;
								}
								for(String reply : replys){
									DXY_Reply dr = new DXY_Reply();
									reply = Jsoup.parse(reply).text(); // 去html标签 ,去换行\t\n\x0B\f\r
									reply = StringEscapeUtils.unescapeHtml4(reply); // 去转义字符，  &gt;  转换成>符号
									reply = reply.trim();
									dr.setContent(reply);
									dr.setTopicId(dxyTopic.getId());
									dxyService.saveReply(dr);
								}
							}
						}
					}
					
					
				}
			}
			
			// 每次翻页停一会，没用
//			try {
//				Thread.sleep(20000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		
	}

}
