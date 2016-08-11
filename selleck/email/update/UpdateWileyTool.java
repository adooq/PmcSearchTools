package selleck.email.update;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class UpdateWileyTool {
	
	/** Wiley按publication title 查询不是精确匹配，比如查"cancer"，会搜到带有"cancer"的所有期刊名字。
	 * 此程序功能就是把期刊列表中包含"cancer"的期刊名去除掉，这样就不会重复查询。
	 * 又例如："Advanced Materials" ，会去除掉"Advanced Energy Materials"、"Advanced Engineering Materials"等。
	 * 期刊列表名"wiley_journal.xlsx" ，生成新的期刊列表wiley_journal_processed.xlsx
	 * @param args
	 */
	public static void main(String[] args) {
		Workbook wb;
		List<PubName> pubNames = null;
		try {
			wb = WorkbookFactory.create(new File("wos all publications list.xlsx"));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			pubNames = new ArrayList<PubName>();
			for (int i = 0; i <= rowCount; i++) { // row从0开始
				Row row = sheet.getRow(i);
				PubName pubName = new PubName();
				String pubNameStr = row.getCell(0).getStringCellValue().trim(); // 期刊名
				pubName.setOriginalStr(pubNameStr);
				pubNameStr = pubNameStr.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ");
				if(pubNameStr.matches("\\s*")){
					continue;
				}
				pubName.setPubNameStr(pubNameStr);
				pubNames.add(pubName);
			}
			
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Collections.sort(pubNames);
		System.out.println(pubNames.size());
		List<PubName> pubNamesCopy = new ArrayList<PubName>(pubNames);
		
		int size = pubNames.size();
		for(int i = 0 ; i < size;i++){
			PubName ps = pubNames.get(i);
			String psStr = ps.getPubNameStr();
			for(int j = i + 1 ; j< size;j++){
				PubName pc = pubNames.get(j);
				String pcStr = pc.getPubNameStr();
				if(pc.contains(ps) && !pcStr.equals(psStr)){
					pubNamesCopy.remove(pc);
				}
			}
		}
		System.out.println(pubNamesCopy.size());
		
		Workbook finalWB = new XSSFWorkbook();
		Sheet sheet = finalWB.createSheet();
		int i = 0;
		for(PubName pn : pubNamesCopy){
			Row row = sheet.createRow(i);
			Cell cell = row.createCell(0);
			cell.setCellType(Cell.CELL_TYPE_STRING);
			cell.setCellValue(pn.getOriginalStr());
			// cell.setCellValue(pn.getPubNameStr());
			System.out.println("1  "+pn.getPubNameStr());
			i++;
		}
		OutputStream os = null;
		try {
			File finalFile = new File("wos all publications list processed.xlsx");
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

	}

}

class PubName implements Comparable<PubName>{
	private String pubNameStr;
	private String originalStr;
	
	public String getPubNameStr() {
		return pubNameStr;
	}
	public void setPubNameStr(String pubNameStr) {
		this.pubNameStr = pubNameStr;
	}
	
	public String getOriginalStr() {
		return originalStr;
	}
	public void setOriginalStr(String originalStr) {
		this.originalStr = originalStr;
	}
	
	@Override
	public int compareTo(PubName o) {
		int num1 = this.getPubNameStr().split(" ").length;
		int num2 = o.getPubNameStr().split(" ").length;
		return num1 - num2;
	}
	
//	@Override
//	public boolean equals(Object obj) {
//		if (obj instanceof PubName) {
//			return ((PubName)obj).getPubNameStr().equals(this.getPubNameStr());
//		}
//		return false;
//	}
//	
//	@Override
//	public int hashCode() {
//		return this.getPubNameStr().hashCode();
//	}
//	
	public boolean contains(PubName p){
		String[] shortnames = p.getPubNameStr().split(" ");
		Pattern pattern;
		for(String shortname : shortnames){
			pattern = Pattern.compile("\\b"+shortname+"\\b",Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(this.getPubNameStr());
			if(!matcher.find()){
				return false;
			}
		}
		return true;
	}
	
}
