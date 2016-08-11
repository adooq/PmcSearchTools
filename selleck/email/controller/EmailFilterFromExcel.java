package selleck.email.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * 从一个包含各种混乱形式的email列表，清理出正确格式的email，并去重。
 * 比如，从selleck网站导出用户手动填的邮箱地址，这个列表一般是很混乱的，清理掉明显不正确的邮箱地址。
 * @author fscai
 *
 */
public class EmailFilterFromExcel {
	public static final String EMAIL_REGEX = "[a-zA-Z0-9][\\w\\-\\.]{1,30}@[a-zA-Z0-9][\\w\\-]*(\\.[\\w\\-]+){0,3}(\\.[a-zA-Z]{2,3})";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Workbook wb;
		try {
			wb = WorkbookFactory.create(new File("chaixia-2014-12-23.xlsx"));
			Sheet sheet = wb.getSheet("Sheet1");
			int rowCount = sheet.getLastRowNum();
			Set<Email> emailSet = new HashSet<Email>();
			// for (int i = 101; i <= 200; i++) { // for test
			for (int i = 0; i <= rowCount; i++) { // row从0开始
				Row row = sheet.getRow(i);
				String emailStr = row.getCell(0).getStringCellValue().trim(); // 期刊名
				Pattern p = Pattern.compile(EMAIL_REGEX);
				Matcher matcher = p.matcher(emailStr);
				while (matcher.find()) {
					String email = matcher.group();
					Email e = new Email(email);
					if(!email.contains("@test.com")){
						emailSet.add(e);
					}
				}
			}
			
//			for(Email e : emailSet){
//				System.out.println(e);
//			}
			System.out.println("final size: "+emailSet.size());
			
			Workbook finalWB = new XSSFWorkbook();
			Sheet writeSheet = finalWB.createSheet();
		    int i = 0;
		    for(Email e : emailSet){
		    	Row row = writeSheet.createRow(i);
		    	Cell urlCell = row.createCell(0);
		    	urlCell.setCellType(Cell.CELL_TYPE_STRING);
				urlCell.setCellValue(e.getEmailStr());				
				i++;
			}
			
			OutputStream os = null;
			try {
				File finalFile = new File("chaixia clean.xlsx");
				if(finalFile.exists()){
					finalFile.delete();
				}
				finalFile.createNewFile();
				os = new FileOutputStream(finalFile);
				finalWB.write(os);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(os != null){
					try {
						os.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}

class Email{
	private String emailStr = "";
	
	public Email (String email){
		this.emailStr = email;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Email){
			return ((Email)obj).getEmailStr().equalsIgnoreCase(this.getEmailStr());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.getEmailStr().toLowerCase().hashCode();
	}
	
	@Override
	public String toString() {
		return this.emailStr;
	}

	public String getEmailStr() {
		return emailStr;
	}

	public void setEmailStr(String emailStr) {
		this.emailStr = emailStr;
	}
	
	
	
	
}
