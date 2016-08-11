package selleck.email.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import common.handle.model.Criteria;
import selleck.email.interest.beans.DictClass;
import selleck.email.service.IDictService;
import selleck.email.service.impl.DictServiceImpl;

public class UpdateBrandDict {
	public static final  List<DictClass> DICT_LIST = getDict();
	/**更新字典表brand_dict
	 * @param args
	 */
	public static void main(String[] args) {
		// Workbook newWorkbook = new HSSFWorkbook();
		IDictService dictService = new DictServiceImpl();
		try {
			Workbook wb = WorkbookFactory.create(new File("e:\\20140506 Product-small-big from YJ - KW.xls"));
			Sheet sheet = wb.getSheet("Sheet2");
			int rowCount = sheet.getLastRowNum();
			System.out.println("rowCount:"+rowCount);
			for(int i=0;i<=rowCount;i++){ // row从0开始
			// for(int i=0;i<=0;i++){ // for test
				Row row = sheet.getRow(i);
				System.out.print("row "+i+"   ");
				String interests = row.getCell(0).getStringCellValue().trim();
				int categoryId = (int)Math.round(row.getCell(1).getNumericCellValue());
				int flag = (int)Math.round(row.getCell(2).getNumericCellValue());
				DictClass dc = new DictClass();
				dc.setCategoryId(categoryId);
				dc.setFlag(flag);
				dc.setInterests(interests);
				if(!DICT_LIST.contains(dc)){
					long now = new java.util.Date().getTime();
					dc.setAddDate(new java.sql.Date(now));
					dc.setKeyword(interests);
					List<DictClass> dicts = getSimilarDicts(dc);
					System.out.println(interests);
					for(DictClass d : dicts){
						dictService.insertDict(d);
					}
				}else{
					System.out.println(interests+"  duplicated categoryId and flag");
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
	
	/**
	 * 产生keyword类似的DictClass
	 * @param dc
	 * @return
	 */
	private static List<DictClass> getSimilarDicts(DictClass dc){
		List<DictClass> dicts = new ArrayList<DictClass>();
		dicts.add(dc);
		
		DictClass dc1 = new DictClass();
		
		// 关键词去掉中间-
		if(dc.getKeyword().contains("-")){
			dc1 = new DictClass();
			dc1.setAddDate(dc.getAddDate());
			dc1.setCategoryId(dc.getCategoryId());
			dc1.setFlag(dc.getFlag());
			dc1.setInterests(dc.getInterests());
			dc1.setKeyword(dc.getKeyword().replaceAll("-", ""));
			dicts.add(dc1);
		}
		
		// 关键词去掉中间空格
		if(dc.getKeyword().contains(" ")){
			dc1 = new DictClass();
			dc1.setAddDate(dc.getAddDate());
			dc1.setCategoryId(dc.getCategoryId());
			dc1.setFlag(dc.getFlag());
			dc1.setInterests(dc.getInterests());
			dc1.setKeyword(dc.getKeyword().replaceAll(" ", ""));
			dicts.add(dc1);
		}
		
		// 关键词中间空格变-
		if(dc.getKeyword().contains(" ")){
			dc1 = new DictClass();
			dc1.setAddDate(dc.getAddDate());
			dc1.setCategoryId(dc.getCategoryId());
			dc1.setFlag(dc.getFlag());
			dc1.setInterests(dc.getInterests());
			dc1.setKeyword(dc.getKeyword().replaceAll(" ", "-"));
			dicts.add(dc1);
		}
		
		// 关键词中间-变空格
		if(dc.getKeyword().contains("-")){
			dc1 = new DictClass();
			dc1.setAddDate(dc.getAddDate());
			dc1.setCategoryId(dc.getCategoryId());
			dc1.setFlag(dc.getFlag());
			dc1.setInterests(dc.getInterests());
			dc1.setKeyword(dc.getKeyword().replaceAll("-", " "));
			dicts.add(dc1);
		}
		
		// 给每个keyword后面加上-
		List<DictClass> dicts2 = new ArrayList<DictClass>(); 
		for(DictClass d : dicts){
			dc1 = new DictClass();
			dc1.setAddDate(d.getAddDate());
			dc1.setCategoryId(d.getCategoryId());
			dc1.setFlag(d.getFlag());
			dc1.setInterests(d.getInterests());
			dc1.setKeyword(d.getKeyword()+"-");
			dicts2.add(dc1);
		}
		
		dicts.addAll(dicts2);
		return dicts;
	}
	
	
	// 取数据库表数据brand_dict
		public static List<DictClass> getDict() {
			List<DictClass> dictMap = null;
			IDictService dictService = new DictServiceImpl();
			dictMap = dictService.selectByExample(new Criteria());
			return dictMap;
		}

}
