package selleck.email.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import selleck.email.pojo.EmailTask;
import selleck.email.pojo.OpenClick;
import selleck.email.service.IOpenClickService;
import selleck.email.service.impl.OpenClickServiceImpl;
import selleck.email.update.tools.JTextArea4Log;
import selleck.utils.Constants;


/**
 * 把从邮件发送平台上下载下来的打开和点击的邮件列表，导入selleck_edm_user_open selleck_edm_user_click表。
 * 要做个客户端界面，让用户直接用。
 * @author fscai
 *
 */
public class OpenClickImport {
	private boolean startFlag = false;//本线程开始标志
	private JTextArea4Log loggerTA;
	
	/* 运行前先要设置的参数 */
	public static final String DIR = "lists"; // 要导入的csv文件所在的文件夹，文件名作为兴趣名
	public static final String DB = Constants.LIFE_SCIENCE_DB; // 选择数据库
	public static final int EMAIL_COLUMN = 0; // email 所在的列，从0开始
	public static final int URL_COLUMN = 1; // email 所在的列，从0开始
	public static final boolean HEADER = true; // 是否有表头
	public static boolean isOpen = true; // 打开还是点击，true打开，false点击。
	public static final IOpenClickService openClickService = new OpenClickServiceImpl(DB);
	
	
	private static final String HARDBOUNCE = "hardbounce";
	private static final String SOFTBOUNCE = "softbounce";
	private static final String UNSUBSCRIBE = "unsubsribe";
	private static final String UNOPEN = "unopen";
	
	// 打开列表表头
	private static final String EMAIL_ID ="EmailID";
	private static final String EMAIL_OPEN ="Email OPEN";
	private static final String DATE ="Date";
	private static final String IP_ADDRESS ="Ip Address";
	
	// 点击列表表头
	private static final String EMAIL_CLICK ="Email CLICK";
	private static final String LINK_URL ="Link URL";
	private static final String CLICK_TIME ="Click Time";
	private static final String CLICK_IP ="Click IP";
	
	
	private static int EMAIL_ID_COLUMN = 0;
	private static int EMAIL_OPEN_COLUMN = 0;
	private static int DATE_COLUMN = 0;
	private static int IP_ADDRESS_COLUMN = 0;
	private static int EMAIL_CLICK_COLUMN = 0;
	private static int LINK_URL_COLUMN = 0;
	private static int CLICK_TIME_COLUMN = 0;
	private static int CLICK_IP_COLUMN = 0;
	
	public static void main(String[] args) {
		final OpenClickImportFrame frame = OpenClickImportFrame.getFrame();
		final OpenClickImport oci = new OpenClickImport();
		oci.loggerTA = frame.getLoggerTA();
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				oci.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				oci.setStartFlag(false);
			}
		});

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = oci.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = oci.isStartFlag();
					}
					
					// 更新邮件发送任务的文件
					File taskFile = new File("task profile.xlsx");
					if(taskFile.exists()){
						oci.updateTasks(taskFile);
					}
					
					
					File fileDir = new File(DIR);
					if(!fileDir.exists() || !fileDir.isDirectory()){
						oci.setStartFlag(false);
						frame.stopMouseClicked();
						return;
					}
					File[] files = fileDir.listFiles();
					String select = frame.getMoveCB().getSelectedItem().toString();
					if (select.equals("点击列表")) {
						isOpen = false;
						oci.importOpenClick(files);
						oci.loggerTA.append("======== 点击列表导入完成 ========\n");
						System.out.println("======== 点击列表导入完成 ========");
					}else if (select.equals("打开列表")) {
						isOpen = true;
						oci.importOpenClick(files);
						oci.loggerTA.append("======== 打开列表导入完成 ========\n");
						System.out.println("======== 打开列表导入完成 ========");
					}else if (select.equals("硬退列表")) {
						oci.importBounce(files , OpenClickImport.HARDBOUNCE);
						oci.loggerTA.append("======== 硬退列表导入完成 ========\n");
						System.out.println("======== 硬退列表导入完成 ========");
					}else if (select.equals("软退列表")) {
						oci.importBounce(files , OpenClickImport.SOFTBOUNCE);
						oci.loggerTA.append("======== 软退列表导入完成 ========\n");
						System.out.println("======== 软退列表导入完成 ========");
					}else if (select.equals("退订列表")) {
						oci.importBounce(files , OpenClickImport.UNSUBSCRIBE);
						oci.loggerTA.append("======== 退订列表导入完成 ========\n");
						System.out.println("======== 退订列表导入完成 ========");
					}else if (select.equals("未打开列表")) {
						oci.importBounce(files , OpenClickImport.UNOPEN);
						oci.loggerTA.append("======== 未打开列表导入完成 ========\n");
						System.out.println("======== 未打开列表导入完成 ========");
					}
					oci.setStartFlag(false);
					frame.stopMouseClicked();
					
				}
			}
		});
		thread.start();
		// importOpenClick();
	}
	
	/**
	 * 更新邮件发送任务的文件。
	 * 与邮件点击或打开列表连接起来能获得更多信息。（EmailTask.emailId = OpenClick.campaignId）
	 * @param file
	 */
	public void updateTasks(File file){
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(file);
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheetAt(0);
		int maxRowCount = sheet.getLastRowNum();
		for (int i = 1; i <= maxRowCount; i++) { // row从0开始 
			Row row = sheet.getRow(i);
			String emailId = row.getCell(0) == null ? "" : row.getCell(0).getStringCellValue();
			String title = row.getCell(1) == null ? "" : row.getCell(1).getStringCellValue();
			String keywords = row.getCell(2) == null ? "" : row.getCell(2).getStringCellValue();
			String date = row.getCell(3) == null ? "" : row.getCell(3).getStringCellValue();
			
			EmailTask et =	new EmailTask();
			et.setEmailId(emailId);
			et.setTitle(title);
			et.setKeywords(keywords);
			et.setDate(date);
			openClickService.updateEmailTasks(et);
		}
	}
	
	public void importBounce(File[] files , String table){
		for(File file : files){
			String fileName = file.getName(); // 文件名，作为兴趣名
			if(fileName.endsWith(".csv")){
				importFromCSV(file,table);
			}else if(fileName.endsWith(".xlsx")){
				importFromExcel(file,table);
			}	
		}
	}
	
	/**
	 * 从CSV文件导入到库
	 * @param file csv file
	 * @param table 导入的表
	 */
	private void importFromCSV(File file , String table){
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String line = "";
			int rowCount = 0;
			while ((line = br.readLine()) != null) {
				if(rowCount == 0 && HEADER){
					rowCount ++;
					continue;
				}else{
					rowCount ++;
				}
				
				String[] columns = line.split(","); // 以 , 分列
				if(columns.length <= EMAIL_COLUMN){
					continue;
				}
				String email = columns[EMAIL_COLUMN];
				if(email.trim().isEmpty()){
					continue;
				}
				if(table.equals(OpenClickImport.HARDBOUNCE)){
					openClickService.insertHardBounce(email);
				}else if(table.equals(OpenClickImport.SOFTBOUNCE)){
					openClickService.insertSoftBounce(email);
				}else if(table.equals(OpenClickImport.UNSUBSCRIBE)){
					openClickService.insertUnsubscribe(email);
				}else if(table.equals(OpenClickImport.UNOPEN)){
					openClickService.insertUnopen(email);
				}
			}
			if(HEADER){
				rowCount -- ;
			}
			this.loggerTA.append(file.getName().replaceAll("\\.csv", "")+" : "+ rowCount+"\n");
			System.out.println(file.getName().replaceAll("\\.csv", "")+" : "+ rowCount);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void importOpenClick(File[] files){
			for(File file : files){
				this.restIds();
				String fileName = file.getName(); // 文件名，作为兴趣名
				if(fileName.endsWith(".csv")){
					importFromCSV(file);
				}else if(fileName.endsWith(".xlsx")){
					importFromExcel(file);
				}	
			}
	}
	
	private void importFromCSV(File file){
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String line = "";
			int rowCount = 0;
			while ((line = br.readLine()) != null) {
				rowCount ++;
				String[] columns = line.split(","); // 以 , 分列
				if(rowCount == 1 && HEADER){ // 表头
					for(int i =0;i<columns.length;i++){
						String c = columns[i];
						if(c.equalsIgnoreCase(OpenClickImport.EMAIL_ID)){
							OpenClickImport.EMAIL_ID_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.EMAIL_OPEN)){
							if(!isOpen){ // 判断一下，如果表头不符合，提示并退出
								this.loggerTA.append(file.getName()+" 请确认是否是点击列表\n");
								System.out.println(file.getName()+" 请确认是否是点击列表");
								return;
							}
							OpenClickImport.EMAIL_OPEN_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.DATE)){
							OpenClickImport.DATE_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.IP_ADDRESS)){
							OpenClickImport.IP_ADDRESS_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.EMAIL_CLICK)){
							if(isOpen){
								this.loggerTA.append(file.getName()+" 请确认是否是打开列表\n");
								System.out.println(file.getName()+" 请确认是否是打开列表");
								return;
							}
							OpenClickImport.EMAIL_CLICK_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.LINK_URL)){
							OpenClickImport.LINK_URL_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.CLICK_TIME)){
							OpenClickImport.CLICK_TIME_COLUMN = i;
						}else if(c.equalsIgnoreCase(OpenClickImport.CLICK_IP)){
							OpenClickImport.CLICK_IP_COLUMN = i;
						}
						
					}
					
					if(OpenClickImport.EMAIL_OPEN_COLUMN == 99 && OpenClickImport.EMAIL_CLICK_COLUMN == 99){ // 说明未找到email列，直接跳出
						this.loggerTA.append(file.getName()+" 未找到邮箱地址的列\n");
						System.out.println(file.getName()+" 未找到邮箱地址的列");
						return;
					}
					continue;
				}
				
				String campaignId = columns[EMAIL_ID_COLUMN];
				
				OpenClick oc = new OpenClick();
				oc.setCampaignId(campaignId);
				
				if(isOpen){
					String email = EMAIL_OPEN_COLUMN >= columns.length ? "" : columns[EMAIL_OPEN_COLUMN];
					String date =  DATE_COLUMN >= columns.length ? "" : columns[DATE_COLUMN];
					String ip = IP_ADDRESS_COLUMN >= columns.length ? "" : columns[IP_ADDRESS_COLUMN];
					oc.setEmail(email);
					oc.setIp(ip);
					oc.setTime(date);
					openClickService.insertOpen(oc);
				}else{
					String email = EMAIL_CLICK_COLUMN >= columns.length ? "" : columns[EMAIL_CLICK_COLUMN];
					String url = LINK_URL_COLUMN >= columns.length ? "" : columns[LINK_URL_COLUMN];
					String date = CLICK_TIME_COLUMN >= columns.length ? "" : columns[CLICK_TIME_COLUMN];
					String ip = CLICK_IP_COLUMN >= columns.length ? "" : columns[CLICK_IP_COLUMN];
					oc.setEmail(email);
					oc.setUrl(url);
					oc.setTime(date);
					oc.setIp(ip);
					openClickService.insertClick(oc);
				}
			}
			if(HEADER){
				rowCount -- ;
			}
			this.loggerTA.append(file.getName().replaceAll("\\.csv", "")+" : "+ rowCount+"\n");
			System.out.println(file.getName().replaceAll("\\.csv", "")+" : "+ rowCount);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private void importFromExcel(File file){
		Workbook wb = null;
		int rowCount = 0;
		try {
			wb = WorkbookFactory.create(file);
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheetAt(0);
		int maxRowCount = sheet.getLastRowNum();
		for (int i = 0; i <= maxRowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
			rowCount ++;
			if(rowCount == 1 && HEADER){ // 表头
				Row headRow = sheet.getRow(0);
				int cellNum = headRow.getLastCellNum();
				for(int j =0;j<cellNum;j++){
					Cell headCell = headRow.getCell(j);
					String c = headCell.getStringCellValue();
					if(c.equalsIgnoreCase(OpenClickImport.EMAIL_ID)){
						OpenClickImport.EMAIL_ID_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.EMAIL_OPEN)){
						if(!isOpen){ // 判断一下，如果表头不符合，提示并退出
							this.loggerTA.append(file.getName()+" 请确认是否是点击列表\n");
							System.out.println(file.getName()+" 请确认是否是点击列表");
							return;
						}
						OpenClickImport.EMAIL_OPEN_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.DATE)){
						OpenClickImport.DATE_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.IP_ADDRESS)){
						OpenClickImport.IP_ADDRESS_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.EMAIL_CLICK)){
						if(isOpen){
							this.loggerTA.append(file.getName()+" 请确认是否是打开列表\n");
							System.out.println(file.getName()+" 请确认是否是打开列表");
							return;
						}
						OpenClickImport.EMAIL_CLICK_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.LINK_URL)){
						OpenClickImport.LINK_URL_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.CLICK_TIME)){
						OpenClickImport.CLICK_TIME_COLUMN = j;
					}else if(c.equalsIgnoreCase(OpenClickImport.CLICK_IP)){
						OpenClickImport.CLICK_IP_COLUMN = j;
					}
					
				}
				if(OpenClickImport.EMAIL_OPEN_COLUMN == 99 && EMAIL_CLICK_COLUMN == 99){ // 说明未找到email列，直接跳出
					this.loggerTA.append(file.getName()+" 未找到邮箱地址的列\n");
					System.out.println(file.getName()+" 未找到邮箱地址的列");
					return;
				}
				continue;
			}
			try{
				Row row = sheet.getRow(i);
				String campaignId = row.getCell(EMAIL_ID_COLUMN) == null ? "" : row.getCell(EMAIL_ID_COLUMN).getStringCellValue();
				
				OpenClick oc = new OpenClick();
				oc.setCampaignId(campaignId);
				
				if(isOpen){
					String email = row.getCell(EMAIL_OPEN_COLUMN) == null ? "" :  row.getCell(EMAIL_OPEN_COLUMN).getStringCellValue();
					String date = row.getCell(DATE_COLUMN) == null ? "" :  row.getCell(DATE_COLUMN).getStringCellValue();
					String ip = row.getCell(IP_ADDRESS_COLUMN) == null ? "" :  row.getCell(IP_ADDRESS_COLUMN).getStringCellValue();
					oc.setEmail(email);
					oc.setIp(ip);
					oc.setTime(date);
					openClickService.insertOpen(oc);
				}else{
					String email = row.getCell(EMAIL_CLICK_COLUMN) == null ? "" :  row.getCell(EMAIL_CLICK_COLUMN).getStringCellValue();
					String url = row.getCell(LINK_URL_COLUMN) == null ? "" :  row.getCell(LINK_URL_COLUMN).getStringCellValue();
					String date = row.getCell(CLICK_TIME_COLUMN) == null ? "" :  row.getCell(CLICK_TIME_COLUMN).getStringCellValue();
					String ip = row.getCell(CLICK_IP_COLUMN) == null ? "" :  row.getCell(CLICK_IP_COLUMN).getStringCellValue();
					oc.setEmail(email);
					oc.setUrl(url);
					oc.setTime(date);
					oc.setIp(ip);
					openClickService.insertClick(oc);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(HEADER){
			rowCount -- ;
		}
		this.loggerTA.append(file.getName().replaceAll("\\.xlsx", "")+" : "+ rowCount+"\n");
		System.out.println(file.getName().replaceAll("\\.xlsx", "")+" : "+ rowCount);
	}
	
	private void importFromExcel(File file,String table){
		Workbook wb = null;
		int rowCount = 0;
		try {
			wb = WorkbookFactory.create(file);
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheetAt(0);
		int maxRowCount = sheet.getLastRowNum();
		for (int i = 0; i <= maxRowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
			if(rowCount == 0 && HEADER){
				rowCount ++;
				continue;
			}else{
				rowCount ++;
			}
			try{
				Row row = sheet.getRow(i);
				Cell cell = row.getCell(EMAIL_COLUMN);
				String email = cell.getStringCellValue();
				if(email.trim().isEmpty()){
					continue;
				}
				if(table.equals(OpenClickImport.HARDBOUNCE)){
					openClickService.insertHardBounce(email);
				}else if(table.equals(OpenClickImport.SOFTBOUNCE)){
					openClickService.insertSoftBounce(email);
				}else if(table.equals(OpenClickImport.UNSUBSCRIBE)){
					openClickService.insertUnsubscribe(email);
				}else if(table.equals(OpenClickImport.UNOPEN)){
					openClickService.insertUnopen(email);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(HEADER){
			rowCount -- ;
		}
		this.loggerTA.append(file.getName().replaceAll("\\.xlsx", "")+" : "+ rowCount+"\n");
		System.out.println(file.getName().replaceAll("\\.xlsx", "")+" : "+ rowCount);
	}
	
	/**
	 * 每读取完一个文件，重置表头的列标记
	 */
	private void restIds(){
		EMAIL_ID_COLUMN = 99;
		EMAIL_OPEN_COLUMN = 99;
		DATE_COLUMN = 99;
		IP_ADDRESS_COLUMN = 99;
		EMAIL_CLICK_COLUMN = 99;
		LINK_URL_COLUMN = 99;
		CLICK_TIME_COLUMN = 99;
		CLICK_IP_COLUMN = 99;
	}
	
	public boolean isStartFlag() {
		return startFlag;
	}

	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
	
}
