package selleck.email.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import common.handle.model.Criteria;
import selleck.email.pojo.DXY_Reply;
import selleck.email.service.IDXYService;
import selleck.email.service.impl.DXYServiceImpl;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.Constants;
import selleck.utils.StringUtils;

/**
 * 
 * @author 从数据库里搜email
 *
 */
public class FindEmailFromDB {
	// public static final String EXCEL = "丁香园-核酸技术帖子.xlsx";
	static Set<String> emailSet = new HashSet<String>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IDXYService dxyService = new DXYServiceImpl(Constants.LOCAL);

		int startIndex = 1; // 起始id
		int step = 10000;
		while(startIndex <= 1162615){ // MAX(id) 
		// while (startIndex == 0) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" id >= " + startIndex + " and id < "+(startIndex+step));
			startIndex += step;
			
			List<DXY_Reply>  replies = dxyService.selectReply(criteria);
			for(DXY_Reply reply : replies){
				String content = reply.getContent();
				List<String> emails = ParserUtils.findInContent(StringUtils.EMAIL_REGEX, content);
				emailSet.addAll(emails);
			}
		}
		
		System.out.println(emailSet.size());
		for(String e : emailSet){
			System.out.println(e);
		}
	}
	
	private static void findEmails(String excel){
		Workbook wb = null;
		try {
			// wb = WorkbookFactory.create(new File("lab list test.xlsx"));
			wb = new XSSFWorkbook(new FileInputStream(excel));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			// for (int i = 0; i <= 0; i++) { // for test
			// for (int i = 0; i <= rowCount; i++) { // row从0开始
			for (int i = 0; i <= rowCount; i++) { // row从0开始
				try {
					Row row = sheet.getRow(i);
					String title = row.getCell(0).getStringCellValue().trim(); // 标题
					String content = row.getCell(1).getStringCellValue().trim(); // 内容
					// System.out.println(content);
					List<String> emails = ParserUtils.findInContent(StringUtils.EMAIL_REGEX, title+" "+content);
					for(String email : emails){
						// System.out.println(email);
						emailSet.add(email.toLowerCase());
					}
				} catch (Exception e) {
					// e.printStackTrace();
					continue;
				}
				
			}
			
			
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}finally{
			if(wb != null){
        		try {
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
		}
	}

}
