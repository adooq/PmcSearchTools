package selleck.email.controller;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import common.handle.frame.FrameClass;
import common.handle.frame.LineDocument;
import selleck.email.update.tools.HMARobot;
import selleck.email.update.tools.JTextArea4Log;
import selleck.email.update.tools.ParserUtils;


/**
 * 根据产品，去google scholar上搜结果数量
 * @author fscai
 *
 */
public class ProductGoogleScholarSearchForJiaoLei {
	private String defaultCookie = "HSID=AEaQNfYjBsdtm_1tD; SSID=AKbYdtY_GI-KP8cWS; APISID=Sr_zm-xezv184VqM/A_qc8rGWVVvMxpEYc; SAPISID=i8-aVvOy7L02UIpm/AngNn8kIlpW1rLTjA; SID=DQAAAKMBAACIxJYZjWtlqAGn_5nddPWpVso2Cxne0r8iiFhR3xiy3McXg7xEu1--3ZZYN00hSckoiHpzGW0-sGjQ_8eHR0refKRFS1ngtEs7BdYYjCtgcoAumPnsogBHusZz-TUd6YJGCqZsed8NA9ajDNtLc1pGYGnY0QRkSTIiWsn_FWeJs9e6J7akcRZ2hYv0rtyKybW2ZGLzgPknoemPv_L3RgJC3XVMu7MpiCJLMfC-1V0JF9j-6fhzQ-x_Trxa14njPFMDT0VRBbw8pJZhxFaTGISi4sC8-x0TlMcMdvC1b449AiNkR-NJp-tXxTfAXCyVXedardRHdih61ypuGPROExtRZcLy3dAI4PsFF2a2L3VuZTG_odSTSExwuK6pheEjnlZKxsNuqG_-UXrlupC6Iz_xkVYX-nvxZEpLLIbjvu32gTX3j6obBdJ9BhL7hdTm2WbIsoY89KJcFrIqxxoqRBk8j3PnjLVMcXnyCOGkGai3jJ9aNHZ9GjejS8ZX8hEQ0WjG1Nea84b3PLDNC8o-xXp39GpTWkMIOWP46TIRKapYvAkv2qeVseTnHpsqD6hYl3I; PREF=ID=1111111111111111:U=43d8662080e9f019:LD=zh-CN:NW=1:TM=1397549951:LM=1434604634:GM=1:S=OH9uHHi6mm3zaNQ4; GSP=NW=1:A=kg7M8A:CPTS=1434618706:LM=1434618706:S=bUtNPh9o8izvPZCt; NID=68=Vc2yBCb9lzn5C87UKzPBmvmtok1iQ1qOoffxgLG6203MgN_XunxPYaJ5B6VmnnQxT49INQvEXDiLBMSC2hNmVCtqZD1GhEXSCKcwPro_61pJQJsvvlJWzJB187Nw9U_907AF6Gt_4JWB65vRODduj4lhmO25Kw";
	private String defaultCookieDomain = ".google.com"; 
	
	public static long INTERVAL = 10000; // 翻页和查询文章请求的间隔ms
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	private String cookie;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final GSFrame frame = GSFrame.getFrame();
		final ProductGoogleScholarSearchForJiaoLei gs = new ProductGoogleScholarSearchForJiaoLei();
		gs.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				gs.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				gs.setStartFlag(false);
			}
		});
		
		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = gs.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = gs.isStartFlag();
					}
					
					String excelName = frame.getExcelText().getText();
					gs.loadProduct(new File(excelName));
					
					gs.setStartFlag(false);
					frame.stopMouseClicked();
					gs.loggerTA.append("======== 完成产品搜索 ========");
				}
			}
		});
		gmailThread.start();
	}
	
	/**
	 * 
	 * @param excel 有表头，4列，分别为cat,name,xxx,citation
	 */
	private void loadProduct(File excel){
		// cookie = getBaseCookie();
		
		Workbook wb;
		BufferedWriter bw = null;
		try {
			File finalCSV = new File("final.csv"); // 最终输出CSV文件
			if (finalCSV.exists()) {
				finalCSV.delete();
			}
			finalCSV.createNewFile();
			
			bw = new BufferedWriter(new FileWriter(finalCSV, true));
			String newLine = "\"cat\",\"productName\",\"citation\",\"GoogleScholar\""; // 表头
			bw.write(newLine);
			bw.newLine();
			wb = WorkbookFactory.create(excel);
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			for (int i = 1; i <= rowCount; i++) { // row从0开始
				Row row = sheet.getRow(i);
				String cat = row.getCell(0).getStringCellValue().trim(); // cat
				String productName = row.getCell(1).getStringCellValue().trim(); // 产品名
				String citation = String.valueOf(row.getCell(3).getNumericCellValue()); // citation
				int num = queryInGoogleScholar(productName);
				newLine = "\"" +cat + "\",\"" +productName + "\",\"" + citation + "\",\"" + num+"\"";
				System.out.println(newLine);
				bw.write(newLine);
				bw.newLine();
			}
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
	
	private int queryInGoogleScholar(String productName){
		int num;
		String productName2 = productName.trim().replaceAll("\\s+", "+"); // 把空格变成加号
		// https://scholar.google.com/scholar?q=%22Motesanib+Diphosphate+AMG-706%22&hl=zh-CN&as_sdt=0,5&as_ylo=2014
		// https://scholar.google.com/scholar?as_q=&as_epq=Motesanib+Diphosphate+AMG-706&as_oq=&as_eq=&as_occt=any&as_sauthors=&as_publication=&as_ylo=2014&as_yhi=&btnG=&hl=zh-CN&as_sdt=0%2C5
		// https://scholar.google.com/scholar?as_ylo=2014&q=%22Letrozole%22+AND+%22selleck%22+OR+%22selleckchem%22&hl=zh-CN&as_sdt=0,1
		String url = "https://scholar.google.com/scholar?as_ylo=2014&q=%22"+productName2+"%22+AND+%22selleck%22+OR+%22selleckchem%22&hl=zh-CN&as_sdt=0,1";
		String htmlStr = selleck.utils.HTTPUtils.getCookieUrlAndHtml(url, defaultCookie,defaultCookieDomain, HTTPUtils.GET, null).get("html");
		try {
			Thread.sleep(INTERVAL + Math.round(Math.random()*10000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// String htmlStr = selleck.utils.HTTPUtils.getCookieUrlAndHtml(url, cookie,"scholar.google.com", HTTPUtils.GET, null).get("html");
		System.out.println(htmlStr);
		if(htmlStr == null || htmlStr.isEmpty()){
			// 有异常，换IP
			HMARobot robot = new HMARobot();
			robot.changeIP(loggerTA);
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// 重新获得cookie
			defaultCookie = getBaseCookie();
			
			return queryInGoogleScholar(productName);
		}else if(htmlStr.contains("均不相符")){
			num = 0;
		}else if(!htmlStr.contains("条结果")){
			// 有异常，换IP
			HMARobot robot = new HMARobot();
			robot.changeIP(loggerTA);
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return queryInGoogleScholar(productName);
		}else{
			String numStr = ParserUtils.findInContent("[\\s\\d,]+条结果", htmlStr).get(0);
			num = Integer.valueOf(ParserUtils.findInContent("\\d+", numStr).get(0));
		}

		loggerTA.append(productName + " 找到 "+num+"条结果\n");
		return num;
	}
	
	private String getBaseCookie(){
		String domain = "https://scholar.google.com";
		return selleck.utils.HTTPUtils.getCookieUrlAndHtml(domain, null,null, HTTPUtils.GET, null).get("cookie");
	}
	
	public boolean isStartFlag() {
		return startFlag;
	}

	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}

	public JTextArea4Log getLoggerTA() {
		return loggerTA;
	}

	public void setLoggerTA(JTextArea4Log loggerTA) {
		this.loggerTA = loggerTA;
	}
}


@SuppressWarnings("serial")
class GSFrame extends FrameClass{
	protected JTextField excelText; // excel 文件名 label
//	protected JTextField intervalText; // 采集时间间隔  label
//	protected JComboBox<String> accountSelect; // 账号选择下拉菜单
//	protected JComboBox<String> dbSelect; // 选择存入的数据库，life science , material science , chemistry
//	protected JTextField pubLabel; // 用来填写期刊excel里需要抓的范围，形如:  201-400
	
	private GSFrame(){
		initControls();
		initLayout();
	}
	
	public static final GSFrame getFrame(){
		GSFrame frame = new GSFrame();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.getLoggerTA().setDocument(
			new LineDocument(frame.getLoggerTA(), 300));
		return frame;
	}
	
	public void stopMouseClicked(){;
		startBT.setEnabled(true);
		stopBT.setEnabled(false);
	}	
	public void startMouseClicked(){
		startBT.setEnabled(false);
		stopBT.setEnabled(true);	
	}
	
	private void initControls(){
		setTitle("Google Scholar 搜索");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(new ImageIcon("icon.png").getImage());
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				btnExitMouseClicked();
			}
		});
		
		moveLB = new JLabel("期限:");
		startBT = new JButton("开始");
		stopBT = new JButton("停止");
		stopBT.setEnabled(false);
		
		loggerTA = new JTextArea4Log();
		loggerTA.setEditable(false);
		loggerTA.setAutoscrolls(true);
		loggerTA.setLineWrap(true); 
		loggerTA.invalidate();
		
		loggerSP = new JScrollPane();
		loggerSP.setPreferredSize(new Dimension(800, 600));
		loggerSP.setAutoscrolls(true);
		loggerSP.setViewportView(loggerTA);
		
		excelText =  new JTextField("citation小于5产品.xls",20); // excel 文件名 label
	}
	
	private void btnExitMouseClicked() {
		int choice = JOptionPane.showConfirmDialog(this, 
				"确定要关闭并退出本软件吗？",
				"退出提示", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null);
		if (choice == JOptionPane.NO_OPTION) {
			return;
		}
		System.exit(0);
		
	}
	
	private void initLayout() {
		JPanel pLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pLable.add(startBT);
		pLable.add(stopBT);
		
		JPanel textLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
		textLable.add(new JLabel("产品excel文件名:"));
		textLable.add(excelText);
		
		JPanel pLogger = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pLogger.add(loggerSP);

		JPanel pStats = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		GroupLayout layout = new GroupLayout(getContentPane());
		layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(
						layout.createParallelGroup().addGroup(
						layout.createSequentialGroup().addComponent(pLable)).addGroup(
						layout.createSequentialGroup().addComponent(textLable)).addGroup(
						layout.createSequentialGroup()).addComponent(pLogger).addGroup(
						layout.createSequentialGroup().addComponent(pStats))));
		layout.setVerticalGroup(layout.createParallelGroup().addGroup(
						layout.createSequentialGroup().addGroup(
						layout.createParallelGroup().addComponent(pLable)).addGroup(
						layout.createSequentialGroup().addComponent(textLable)).addGroup(
						layout.createParallelGroup()).addComponent(pLogger).addGroup(
						layout.createParallelGroup().addComponent(pStats))));
		
		setLayout(layout);
		pack();
	}

	public JTextField getExcelText() {
		return excelText;
	}

	public void setExcelText(JTextField excelText) {
		this.excelText = excelText;
	}
	
}

