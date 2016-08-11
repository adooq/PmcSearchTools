package selleck.email.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class CountPubmedEmail {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(new File("pubmed email.xlsx"));
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheet("Sheet1");
		int rowCount = sheet.getLastRowNum();
		System.out.println("rowCount:" + rowCount);
		Set<String> emailSet = new HashSet<String>();
		for (int i = 0; i <= rowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
			Row row = sheet.getRow(i);
			String emails = row.getCell(0).getStringCellValue();
			// [1]a.eder@uke.de|[2]i.vollert@uke.de|[3]ar.hansen@uke.de|[4]t.eschenhagen@uke.de|
			String[] emailArr = emails.split("\\|");
			for(String email : emailArr){
				if(email.trim().isEmpty()){
					continue;
				}
				email = email.replaceAll("\\[\\d+\\]", "");
				// System.out.println(email);
				emailSet.add(email);
			}
		}
		
		BufferedWriter bw = null;
		try{
			File finalCSV = new File("pubmed.csv"); // 最终输出CSV文件
			if (!finalCSV.exists()) {
				finalCSV.createNewFile();
			}
	
			bw = new BufferedWriter(new FileWriter(finalCSV, true));
			for(String e : emailSet){
				bw.write(e);
				bw.newLine();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(bw != null){
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
