package selleck.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class UpdateExcel {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Workbook wb = null;
		try {
			// wb = WorkbookFactory.create(new File("lab list test.xlsx"));
			wb = new XSSFWorkbook(new FileInputStream("lab list test.xlsx"));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			// for (int i = 0; i <= 0; i++) { // for test
			// for (int i = 0; i <= rowCount; i++) { // row从0开始
			for (int i = 0; i <= rowCount; i++) { // row从0开始
				try {
					Row row = sheet.getRow(i);
					String labUrl = row.getCell(0).getStringCellValue().trim(); // 期刊名
					// System.out.println(labUrl);
					if(row.getCell(1) == null){
						System.out.println("未读");
						Cell cell2 = row.createCell(1 , Cell.CELL_TYPE_STRING);
						cell2.setCellValue("1");
					}else{
						System.out.println("已读");
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
			}
			
			FileOutputStream out = null;
	        try {
	            out = new FileOutputStream("lab list test.xlsx");
	            wb.write(out);
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            try {
	                out.close();
	            } catch (IOException e) {
	                e.printStackTrace();
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
	
	public static void main2(String[] args) {
        String fileToBeRead = "lab list test.xlsx"; // excel位置
        XSSFWorkbook workbook = null;
        try {
        	workbook = new XSSFWorkbook(new FileInputStream(fileToBeRead));
            Sheet sheet = workbook.getSheet("Sheet1");
 
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (null == row) {
                    continue;
                } else {
                    Cell cell = row.getCell(0);
                    if (null == cell) {
                        continue;
                    } else {
                        System.out.println(cell.getStringCellValue());
						Cell cell2 = row.createCell(1 , Cell.CELL_TYPE_STRING);
						cell2.setCellValue("1");
                    }
                }
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(fileToBeRead);
                workbook.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
        	if(workbook != null){
        		try {
					workbook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
        
        
    }
}
