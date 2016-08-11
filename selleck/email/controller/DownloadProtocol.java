package selleck.email.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;

import selleck.email.update.tools.ParserUtils;
import selleck.utils.HTTPUtils;

/**
 * 从丁香通下载热门实验protocol，保存到excel里。For 吴明明、马列
 * @author fscai
 *
 */
public class DownloadProtocol {
	public static void main(String[] args){
		// testUrl();
		test();
	}
	
	private static void testUrl(){
		String articleUrl = "http://www.biomart.cn/experiment/87.htm";
		Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(articleUrl, null ,null, HTTPUtils.GET , null);
		String articleHtml = articleMap.get("html");
		Protocol protocol = ProtocolParser.parse(articleHtml);
		System.out.println("ref: "+protocol);
	}
	
	public static void test() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("protocols");
		Row row;
		Cell cell;
		CreationHelper createHelper = workbook.getCreationHelper();
		
		// 超链接字体样式
		XSSFCellStyle hlinkstyle = workbook.createCellStyle();
		XSSFFont hlinkfont = workbook.createFont();
		hlinkfont.setUnderline(XSSFFont.U_SINGLE);
		hlinkfont.setColor(HSSFColor.BLUE.index);
		hlinkstyle.setFont(hlinkfont);
		
		XSSFCellStyle commonStyle = workbook.createCellStyle();    
		commonStyle.setWrapText(true);
		commonStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		
		
		// 建表头
		row = sheet.createRow(0);
		cell = row.createCell((short) 0);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("实验名称");
		cell = row.createCell((short) 1);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("标签");
		cell = row.createCell((short) 2);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("实验简介");
		cell = row.createCell((short) 3);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("tab");
		cell = row.createCell((short) 4);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("实验方法原理");
		cell = row.createCell((short) 5);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("实验材料");
		cell = row.createCell((short) 6);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("试剂、试剂盒");
		cell = row.createCell((short) 7);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("仪器耗材");
		cell = row.createCell((short) 8);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("实验步骤");
		cell = row.createCell((short) 9);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("注意事项");
		cell = row.createCell((short) 10);
		cell.setCellType(Cell.CELL_TYPE_STRING);
		cell.setCellValue("其他");
		
		// 设置列宽
		sheet.setColumnWidth(2,30*256); // 实验简介
		sheet.setColumnWidth(4,40*256); // 实验方法原理
		sheet.setColumnWidth(8,40*256); // 实验步骤
		
		List<String> urls = findUrls();
		int rowIndex = 1;
		for(String url : urls){
		// for(int i = 0;i<5;i++){ String url = urls.get(i); // for test
			Map<String,String> pageMap = HTTPUtils.getCookieUrlAndHtml(url, null, null, HTTPUtils.GET, null);
			String pageHtml = pageMap.get("html");
			if(pageHtml != null){
				Protocol protocol = ProtocolParser.parse(pageHtml);
				row = sheet.createRow(rowIndex);
				
				// 实验名称
				cell = row.createCell(0, Cell.CELL_TYPE_STRING);
				cell.setCellStyle(commonStyle);
				cell.setCellValue(protocol.getName());
				
				// 标签
				cell = row.createCell(1, Cell.CELL_TYPE_STRING);
				cell.setCellStyle(commonStyle);
				cell.setCellValue(protocol.getTag());
				
				// 实验简介
				cell = row.createCell(2, Cell.CELL_TYPE_STRING);
				cell.setCellStyle(commonStyle);
				cell.setCellValue(new XSSFRichTextString(protocol.getDesc()));
				
				rowIndex ++;
				
				// 各种实验方法
				for(Tab tab : protocol.getTabs()){
					row = sheet.createRow(rowIndex);
					// tab
					cell = row.createCell(3, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getTab()));
					
					// 实验方法原理
					cell = row.createCell(4, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getYuanli()));
					
					// 实验材料
					cell = row.createCell(5, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getCailiao()));
					
					// 试剂、试剂盒
					cell = row.createCell(6, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getShiji()));
					
					// 仪器耗材
					cell = row.createCell(7, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getHaocai()));
					
					// 实验步骤
					cell = row.createCell(8, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getBuzhou()));
					
					// 注意事项
					cell = row.createCell(9, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getZhuyi()));
					
					// 其他
					cell = row.createCell(10, Cell.CELL_TYPE_STRING);
					cell.setCellStyle(commonStyle);
					cell.setCellValue(new XSSFRichTextString(tab.getQita()));
					
					rowIndex ++;
				}
			}
		}
		
		FileOutputStream out = null;
		File excel = new File("protocol.xlsx");
		if(excel.exists()){
			excel.delete();
		}
		try {
			excel.createNewFile();
			out = new FileOutputStream(excel);
			workbook.write(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				workbook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("protocol.xlsx written successfully");
	}
	
	private static List<String> findUrls(){
		List<String> urlList = new ArrayList<String>();
		for(int i=1;i<=118;i++){
		// for(int i=1;i<=1;i++){ // for test
			Map<String,String> listPageMap = HTTPUtils.getCookieUrlAndHtml("http://www.biomart.cn/experiment/hot_"+i+".htm", null, null, HTTPUtils.GET, null);
			String listHtml = listPageMap.get("html");
			if(listHtml == null){
				continue;
			}
			List<String> lis = ParserUtils.findWithPrefixAndSuffix("<li class=\"li1\">", "</a>", listHtml);
			for(String li : lis){
				List<String> urls = ParserUtils.findInContent("http://www\\.biomart\\.cn/experiment/\\d+\\.htm", li);
				if(urls.size() > 0){
					String url = urls.get(0);
					urlList.add(url);
				}
			}
			
		}
		
//		for(String url : urlSet){
//			System.out.println(url);
//		}
		System.out.println(urlList.size());
		return urlList;
	}
	
	public static void main2(String content) {
		List<String> imgs = ParserUtils.findInContent("<img .+?/>", content);
		for(String img : imgs){
			List<String> urls = ParserUtils.findWithPrefixAndSuffix("src=\"", "\"", img);
			
			String imgUrl = urls.size() > 0 ? urls.get(0) : "";
			content = content.replaceAll(img, imgUrl);
		}
		
//		XSSFWorkbook workbook = new XSSFWorkbook();
//		XSSFSheet spreadsheet = workbook.createSheet("protocols");
//		XSSFCell cell;
//		CreationHelper createHelper = workbook.getCreationHelper();
//
//		// 字体样式
//		XSSFCellStyle hlinkstyle = workbook.createCellStyle();
//		XSSFFont hlinkfont = workbook.createFont();
//		hlinkfont.setUnderline(XSSFFont.U_SINGLE);
//		hlinkfont.setColor(HSSFColor.BLUE.index);
//		hlinkstyle.setFont(hlinkfont);
//
//		XSSFHyperlink link = (XSSFHyperlink) createHelper.createHyperlink(Hyperlink.LINK_URL);
//		cell = spreadsheet.createRow(1).createCell((short) 1);
//		cell.setCellValue("图片");
//		link = (XSSFHyperlink) createHelper.createHyperlink(Hyperlink.LINK_FILE);
//		link.setAddress("C:/Users/admin/Desktop/icon_question.png");
//		cell.setHyperlink(link);
//		cell.setCellStyle(hlinkstyle);
		
		
		
		
	
	}

}


class ProtocolParser{
	public static Protocol parse(String content){
		Protocol protocol = new Protocol();
		
		// 实验名称
		List<String> names = ParserUtils.findWithPrefixAndSuffix("<dt><h1>", "</h1></dt>", content);
		String name = names.size() > 0 ? names.get(0) : "";
		name = Jsoup.parse(name).text(); // 去html标签 ,去换行\t\n\x0B\f\r
		name = StringEscapeUtils.unescapeHtml4(name); // 去转义字符，  &gt;  转换成>符号
		name = name.trim();
		protocol.setName(name);
		
		// 标签
		List<String> tags = ParserUtils.findWithPrefixAndSuffix("<p class=\"tagslst\">标签：", "<", content);
		String tag = tags.size() > 0 ? tags.get(0) : "";
		tag = Jsoup.parse(tag).text(); // 去html标签 ,去换行\t\n\x0B\f\r
		tag = StringEscapeUtils.unescapeHtml4(tag); // 去转义字符，  &gt;  转换成>符号
		tag = tag.trim();
		tag = tag.replaceAll("\\s+", " ");
		protocol.setTag(tag);
		
		// 实验简介
		List<String> descs = ParserUtils.findWithPrefixAndSuffix("<p class=\"info\">", "</p>", content);
		String desc = descs.size() > 0 ? descs.get(0) : "";
		desc = desc.replaceAll("<br />", "换行换行换行");
		desc = Jsoup.parse(desc).text(); // 去html标签 ,去换行\t\n\x0B\f\r
		desc = desc.replaceAll("换行换行换行", "\n");
		desc = StringEscapeUtils.unescapeHtml4(desc); // 去转义字符，  &gt;  转换成>符号
		desc = desc.trim();
		protocol.setDesc(desc);
		
		List<Tab> tabList = protocol.getTabs();
		// tab
		List<String> tabLis = ParserUtils.findWithPrefixAndSuffix("<li class=\"it el_btn_s6\"", "</li>", content);
		for(String li : tabLis){
			List<String> tabNames = ParserUtils.findWithPrefixAndSuffix("<span>", "</span>", li);
			for(String tabName : tabNames){
				Tab tab = new Tab();
				tab.setTab(tabName);
				tabList.add(tab);
			}
		}
		
		// 每一个标签里的内容
		List<String> tabTables = ParserUtils.findWithPrefixAndSuffix("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"ct\">", "[\\r\\t]+[ ]*</table>", content);
		for(int i = 0;i<tabTables.size();i++){
			String tabTable = tabTables.get(i);
			Tab currentTab = tabList.get(i);
			
			// 实验方法原理
			List<String> yuanlis = ParserUtils.findWithPrefixAndSuffix("<th>实验方法原理</th>", "</td>", tabTable);
			String yuanli = yuanlis.size() > 0 ? yuanlis.get(0) : "";
			yuanli = replaceImg(yuanli);
			yuanli = yuanli.replaceAll("<br />", "换行换行换行");
			yuanli = Jsoup.parse(yuanli).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			yuanli = yuanli.replaceAll("换行换行换行", "\n");
			yuanli = StringEscapeUtils.unescapeHtml4(yuanli); // 去转义字符，  &gt;  转换成>符号
			yuanli = yuanli.trim();
			currentTab.setYuanli(yuanli);
			
			// 实验材料
			List<String> cailiaos = ParserUtils.findWithPrefixAndSuffix("<th>实验材料</th>", "</td>", tabTable);
			String cailiao = cailiaos.size() > 0 ? cailiaos.get(0) : "";
			cailiao = Jsoup.parse(cailiao).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			cailiao = cailiao.replaceAll("<br />", "\n");
			cailiao = replaceImg(cailiao);
			cailiao = StringEscapeUtils.unescapeHtml4(cailiao); // 去转义字符，  &gt;  转换成>符号
			cailiao = cailiao.trim();
			currentTab.setCailiao(cailiao);
			
			// 试剂、试剂盒
			List<String> shijis = ParserUtils.findWithPrefixAndSuffix("<th>试剂、试剂盒</th>", "</td>", tabTable);
			String shiji = shijis.size() > 0 ? shijis.get(0) : "";
			shiji = replaceImg(shiji);
			shiji = shiji.replaceAll("<br />", "换行换行换行");
			shiji = Jsoup.parse(shiji).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			shiji = shiji.replaceAll("换行换行换行", "\n");
			shiji = StringEscapeUtils.unescapeHtml4(shiji); // 去转义字符，  &gt;  转换成>符号
			shiji = shiji.trim();
			currentTab.setShiji(shiji);
			
			// 仪器、耗材
			List<String> haocais = ParserUtils.findWithPrefixAndSuffix("<th>仪器、耗材</th>", "</td>", tabTable);
			String haocai = haocais.size() > 0 ? haocais.get(0) : "";
			haocai = replaceImg(haocai);
			haocai = haocai.replaceAll("<br />", "换行换行换行");
			haocai = Jsoup.parse(haocai).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			haocai = haocai.replaceAll("换行换行换行", "\n");
			haocai = StringEscapeUtils.unescapeHtml4(haocai); // 去转义字符，  &gt;  转换成>符号
			haocai = haocai.trim();
			currentTab.setHaocai(haocai);
			
			// 实验步骤
			List<String> buzhous = ParserUtils.findWithPrefixAndSuffix("<th>实验步骤</th>", "<div class=\"toggle_mask\">", tabTable);
			String buzhou = buzhous.size() > 0 ? buzhous.get(0) : "";
			buzhou = replaceImg(buzhou);
			buzhou = buzhou.replaceAll("<br />", "换行换行换行");
			buzhou = Jsoup.parse(buzhou).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			buzhou = buzhou.replaceAll("换行换行换行", "\n");
			buzhou = StringEscapeUtils.unescapeHtml4(buzhou); // 去转义字符，  &gt;  转换成>符号
			buzhou = buzhou.trim();
			currentTab.setBuzhou(buzhou);
			
			// 注意事项
			List<String> zhuyis = ParserUtils.findWithPrefixAndSuffix("<th>注意事项</th>", "<div class=\"toggle_mask\">", tabTable);
			String zhuyi = zhuyis.size() > 0 ? zhuyis.get(0) : "";
			zhuyi = replaceImg(zhuyi);
			zhuyi = zhuyi.replaceAll("<br />", "换行换行换行");
			zhuyi = Jsoup.parse(zhuyi).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			zhuyi = zhuyi.replaceAll("换行换行换行", "\n");
			zhuyi = StringEscapeUtils.unescapeHtml4(zhuyi); // 去转义字符，  &gt;  转换成>符号
			zhuyi = zhuyi.trim();
			currentTab.setZhuyi(zhuyi);
			
			// 其他
			List<String> qitas = ParserUtils.findWithPrefixAndSuffix("<th>其他</th>", "<div class=\"toggle_mask\">", tabTable);
			String qita = qitas.size() > 0 ? qitas.get(0) : "";
			qita = replaceImg(qita);
			qita = qita.replaceAll("<br />", "换行换行换行");
			qita = Jsoup.parse(qita).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			qita = qita.replaceAll("换行换行换行", "\n");
			qita = StringEscapeUtils.unescapeHtml4(qita); // 去转义字符，  &gt;  转换成>符号
			qita = qita.trim();
			currentTab.setQita(qita);
			
			
		}

		return protocol;
	}
	
	private static String replaceImg(String content) {
		List<String> imgs = ParserUtils.findInContent("<img .+?/>", content);
		for(String img : imgs){
			List<String> urls = ParserUtils.findWithPrefixAndSuffix("src=\"", "\"", img);
			
			String imgUrl = urls.size() > 0 ? urls.get(0) : "";
			content = content.replaceAll(img, imgUrl);
		}
		return content;
	}
}

class Protocol{
	private String name;
	private String tag;
	private String desc;
	private List<Tab> tabs = new ArrayList<Tab>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public void addTab(Tab tab){
		tabs.add(tab);
	}
	public List<Tab> getTabs() {
		return tabs;
	}
	
}

class Tab{
	private String tab;
	private String yuanli;
	private String cailiao;
	private String shiji;
	private String haocai;
	private String buzhou;
	private String zhuyi;
	private String qita;
	public String getTab() {
		return tab;
	}
	public void setTab(String tab) {
		this.tab = tab;
	}
	public String getYuanli() {
		return yuanli;
	}
	public void setYuanli(String yuanli) {
		this.yuanli = yuanli;
	}
	public String getCailiao() {
		return cailiao;
	}
	public void setCailiao(String cailiao) {
		this.cailiao = cailiao;
	}
	public String getHaocai() {
		return haocai;
	}
	public void setHaocai(String haocai) {
		this.haocai = haocai;
	}
	public String getBuzhou() {
		return buzhou;
	}
	public void setBuzhou(String buzhou) {
		this.buzhou = buzhou;
	}
	public String getZhuyi() {
		return zhuyi;
	}
	public void setZhuyi(String zhuyi) {
		this.zhuyi = zhuyi;
	}
	public String getQita() {
		return qita;
	}
	public void setQita(String qita) {
		this.qita = qita;
	}
	public String getShiji() {
		return shiji;
	}
	public void setShiji(String shiji) {
		this.shiji = shiji;
	}
	
	
}
