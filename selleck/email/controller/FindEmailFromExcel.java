package selleck.email.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import selleck.email.update.tools.ParserUtils;
import selleck.utils.StringUtils;

public class FindEmailFromExcel {
	// public static final String EXCEL = "丁香园-核酸技术帖子.xlsx";
	static Set<String> emailSet = new HashSet<String>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File dir = new File("DXY");
		for(File f : dir.listFiles()){
			String fPath = f.getAbsolutePath();
			findEmails(fPath);
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
