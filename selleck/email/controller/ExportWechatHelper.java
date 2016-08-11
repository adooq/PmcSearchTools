package selleck.email.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 从wxRecover里导出的聊天记录文件夹里导出联系人，两列，昵称和微信唯一id，保存到跟文件夹同名的excel里。
 * @author dat mail:dev.tao@gmail.com
 *
 */
public class ExportWechatHelper {
	public static final String DIR = "擎天柱";
	
	public static void main(String[] args){
		File rsFile = new File(DIR + ".xlsx");
		File dir = new File(DIR);
		File[] dirs = dir.listFiles();
		
		if(rsFile.exists()){
			rsFile.delete();
		}
		Workbook resultWb;
		try {
			rsFile.createNewFile();
			resultWb =  new XSSFWorkbook();
			Sheet rsSheet = resultWb.createSheet();
			int rowIndex = 0;
			
			for(File d : dirs){
				if(d.isFile()){
					continue;
				}
				String dirName = d.getName();
				String nickname = dirName.substring(0,dirName.lastIndexOf("("));
				String wxid = dirName.substring(dirName.lastIndexOf("(") + 1,dirName.length()-1);
				
				System.out.println(nickname + "   " + wxid);
				
				Row rsRow = rsSheet.createRow(rowIndex);
				rsRow.createCell(0).setCellValue(nickname); 
				rsRow.createCell(1).setCellValue(wxid);
				
				rowIndex++;
			}
			resultWb.write(new FileOutputStream(rsFile)); 
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
	}
}
