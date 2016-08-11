package selleck.email.controller;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import common.handle.frame.FrameClass;
import common.handle.frame.LineDocument;
import selleck.email.update.tools.JTextArea4Log;
import selleck.utils.Constants;

@SuppressWarnings("serial")
public class OpenClickImportFrame extends FrameClass{
	protected JTextField excelText; // excel 文件名 label
	protected JTextField intervalText; // 采集时间间隔  label
	protected JTextField pubLabel; // 用来填写期刊excel里需要抓的范围，形如:  201-400
	protected JComboBox<String> rangeSelect; // 时间范围选择下拉菜单
	protected JComboBox<String> dbSelect; // 选择存入的数据库，life science , material science , chemistry
	
	private OpenClickImportFrame(){
		initControls();
		initLayout();
		
	}
	
	public static final OpenClickImportFrame getFrame(){
		OpenClickImportFrame frame = new OpenClickImportFrame();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.getLoggerTA().setDocument(new LineDocument(frame.getLoggerTA(), 300));
		String readme = "导入邮件发送结果报告软件使用说明：\n"
				+ "1. 选择要导入的列表的种类\n"
				+ "2. 要导入的文件放在软件同级目录下的lists目录里，可以放多个，csv、excel都可\n"	
				+ "3. 导入开打列表时， 表头必须为EmailID , Email OPEN , Date , Ip Address \n"
				+ "4. 导入点击列表时， 表头必须为EmailID , Email CLICK , Link URL , Click Time , Click IP \n"
				+ "5. 导入退订类列表时，第一行是表头，第一列是Email\n"
				+ "6. 要导入的邮件发送任务表放在和程序同级目录下，命名为task profile.xlsx。第一行是表头，列依次是EmailID,title,keywords,sending date\n";
		frame.getLoggerTA().append(readme);
		return frame;
	}
	
	public void stopMouseClicked(){
		startBT.setEnabled(true);
		stopBT.setEnabled(false);
	}	
	public void startMouseClicked(){
		startBT.setEnabled(false);
		stopBT.setEnabled(true);	
	}
	
	private void initControls(){
		setTitle("导入邮件报告");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(new ImageIcon("icon.png").getImage());
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				btnExitMouseClicked();
			}
		});
		
		moveLB = new JLabel("导入:");
		startBT = new JButton("开始");
		stopBT = new JButton("停止");
		stopBT.setEnabled(false);
		
		moveCB = new JComboBox<String>(new String[]{"点击列表","打开列表","退订列表","硬退列表","软退列表","未打开列表","主动订阅列表","订单列表"});	
		loggerTA = new JTextArea4Log();
		loggerTA.setEditable(false);
		loggerTA.setAutoscrolls(true);
		loggerTA.setLineWrap(true);
		loggerTA.invalidate();
		
		pubLabel = new JTextField("1-50", 10);
		
		loggerSP = new JScrollPane();
		loggerSP.setPreferredSize(new Dimension(750, 400));
		loggerSP.setAutoscrolls(true);
		loggerSP.setViewportView(loggerTA);
		
		// 选择存入的数据库，life science , material science , chemistry
		dbSelect = new JComboBox<String>(new String[]{Constants.LIFE_SCIENCE_DB,Constants.MATERIAL_SCIENCE_DB,Constants.CHEMISTRY_DB});
		
		publicationText = new JTextField("2015-2015", 10); // 把publicationText当做range year用
		excelText =  new JTextField("wiley_journal_processed.xlsx",15); // excel 文件名 label
		intervalText =  new JTextField("1",3); // 间隔时间 label ， Wiley貌似不会封ip，设置1秒间隔。
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
		pLable.add(moveLB);
		pLable.add(moveCB);
		
		
		JPanel textLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
		textLable.add(new JLabel("期刊excel文件名:"));
		textLable.add(excelText);
		textLable.add(new JLabel("间隔（秒）:"));
		textLable.add(intervalText);
		textLable.add(new JLabel("搜索期刊范围: "));
		textLable.add(this.pubLabel);
		textLable.add(new JLabel("数据库:"));
		textLable.add(dbSelect);
		
//		JPanel nLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
//		nLable.add(nameLB);
//		nLable.add(nameTF);
		
		JPanel pLogger = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pLogger.add(loggerSP);

		JPanel pStats = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		GroupLayout layout = new GroupLayout(getContentPane());
		layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(
						layout.createParallelGroup().addGroup(
						layout.createSequentialGroup().addComponent(pLable)).addGroup(
						// layout.createSequentialGroup().addComponent(textLable)).addGroup(
						layout.createSequentialGroup()).addComponent(pLogger).addGroup(
						layout.createSequentialGroup().addComponent(pStats))));
		layout.setVerticalGroup(layout.createParallelGroup().addGroup(
						layout.createSequentialGroup().addGroup(
						layout.createParallelGroup().addComponent(pLable)).addGroup(
						// layout.createSequentialGroup().addComponent(textLable)).addGroup(
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

	public JTextField getIntervalText() {
		return intervalText;
	}

	public void setIntervalText(JTextField intervalText) {
		this.intervalText = intervalText;
	}

	public JTextField getPubLabel() {
		return pubLabel;
	}

	public void setPubLabel(JTextField pubLabel) {
		this.pubLabel = pubLabel;
	}

	public JComboBox<String> getDbSelect() {
		return dbSelect;
	}

	public void setDbSelect(JComboBox<String> dbSelect) {
		this.dbSelect = dbSelect;
	}
	
	
}
