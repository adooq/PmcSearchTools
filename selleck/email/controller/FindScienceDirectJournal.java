package selleck.email.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;

import selleck.email.update.tools.ParserUtils;

public class FindScienceDirectJournal {

	/**从science direct 期刊列表页(http://www.sciencedirect.com/science/journals/sub/531/532)下载期刊名和期刊链接
	 * http://www.sciencedirect.com/science/journals/sub/531/532 生命科学
	 * http://www.sciencedirect.com/science/journals/sub/materialsscience  材料科学
	 * 期刊列表页上，找到包含期刊列表的tbody，然后把里面的内容复制到e:\journals.html。
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		FileReader reader = new FileReader ("e:\\journals.html");
		StringBuilder sb = new StringBuilder();
	    BufferedReader br = new BufferedReader(reader);
	    String line;
	    while ( (line=br.readLine()) != null) {
	      sb.append(line);
	    }
	    String htmlStr = sb.toString();
	    
	    Workbook finalWB = new XSSFWorkbook();
		Sheet sheet = finalWB.createSheet();
	    List<String> list = ParserUtils.findWithPrefixAndSuffix("<span>" , "</span>" , htmlStr);
	    int i = 0;
	    for(String j : list){
	    	Row row = sheet.createRow(i);
	    	String regex = "/science/journal/\\p{Alnum}+";
			Pattern p = Pattern.compile(regex);
			Matcher matcher = p.matcher(j);
			if (matcher.find()) {
				// http://www.sciencedirect.com/science/journal/09442006
				Cell urlCell = row.createCell(0);
				urlCell.setCellType(Cell.CELL_TYPE_STRING);
				urlCell.setCellValue("http://www.sciencedirect.com"+matcher.group());
				
				Cell cell = row.createCell(1);
				cell.setCellType(Cell.CELL_TYPE_STRING);
				String journal = Jsoup.parse(j).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				cell.setCellValue(journal);
			}
			
			i++;
		}
	    
	    br.close();
	    
		OutputStream os = null;
		try {
			File finalFile = new File("sd journal.xlsx");
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
