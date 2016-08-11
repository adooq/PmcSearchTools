package selleck.email.update.frame;


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
public class UpdateKeywordFrame extends FrameClass{
	protected JTextField excelText; // excel 文件名 label
	protected JTextField intervalText; // 采集时间间隔  label
	protected JComboBox<String> accountSelect; // 账号选择下拉菜单
	protected JComboBox<String> dbSelect; // 选择存入的数据库，life science , material science , chemistry
	protected JTextField pubLabel; // 用来填写关键词excel里需要抓的范围，形如:  1-100
	protected JTextField fromYearText; // 起止年限
	protected JTextField toYearText; // 起止年限
	protected JLabel yearLB;
	
	
	private UpdateKeywordFrame(){
		initControls();
		initLayout();
	}
	
	public static final UpdateKeywordFrame getFrame(){
		UpdateKeywordFrame frame = new UpdateKeywordFrame();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.getLoggerTA().setDocument(
			new LineDocument(frame.getLoggerTA(), 300));
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
		setTitle("更新关键词");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(new ImageIcon("icon.png").getImage());
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				btnExitMouseClicked();
			}
		});
		
		startBT = new JButton("开始");
		stopBT = new JButton("停止");
		stopBT.setEnabled(false);
		yearLB = new JLabel("期限:");
		
		moveCB = new JComboBox<String>(new String[]{"GoogleScholar上搜索关键词","在文献库中搜索文章标题"});
		// 账号下拉菜单里的值必须和wos accounts.properties里对应的账号前缀保持一致
		accountSelect = new JComboBox<String>(new String[]{"squ","twu","utm","uabc","swin","uwsuper","mahidol","lclark","tamiu","mun","bangor","npust","waldenu"});
		
		// 选择存入的数据库，life science , material science , chemistry
		dbSelect = new JComboBox<String>(new String[]{Constants.LIFE_SCIENCE_DB,Constants.MATERIAL_SCIENCE_DB,Constants.CHEMISTRY_DB});
		
		loggerTA = new JTextArea4Log();
		loggerTA.setEditable(false);
		loggerTA.setAutoscrolls(true);
		loggerTA.setLineWrap(true); 
		loggerTA.invalidate();
		
		pubLabel = new JTextField("1-200", 10);
		
		loggerSP = new JScrollPane();
		loggerSP.setPreferredSize(new Dimension(800, 600));
		loggerSP.setAutoscrolls(true);
		loggerSP.setViewportView(loggerTA);
		
//		publicationText = new JTextField("2009/01/01", 10); // 把publicationText当做startEntrez用
//		cookieText =  new JTextField("2014/06/30",10); // 把cookieText当做endEntrez用
		excelText =  new JTextField("keyword.xlsx",20); // excel 文件名 label
//		excelText =  new JTextField("WOS MATERIALS SCIENCE journal list.xlsx",20); // excel 文件名 label
		intervalText =  new JTextField("30",3); // excel 文件名 label
		fromYearText =  new JTextField("2014",3); // 在google scholar上搜的起始年份
		toYearText =  new JTextField("2015",3); // 在google scholar上搜的终止年份
	}
	
	private void btnExitMouseClicked() {
		int choice = JOptionPane.showConfirmDialog(this, 
				"确定要关闭并退出本软件吗？",
				"退出提示", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null);
		if (choice == JOptionPane.NO_OPTION) {
			return;
		}
		loggerTA.closefw();
		System.exit(0);
		
	}
	
	private void initLayout() {
		JPanel pLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pLable.add(startBT);
		pLable.add(stopBT);
		pLable.add(moveCB);
		pLable.add(new JLabel("年限:"));
		pLable.add(fromYearText);
		pLable.add(new JLabel("-"));
		pLable.add(toYearText);
		pLable.add(new JLabel("WOS账号"));
		pLable.add(accountSelect);
		pLable.add(new JLabel("间隔（秒）:"));
		pLable.add(intervalText);
		
		JPanel textLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
//		textLable.add(new JLabel("发布时间:"));
//		textLable.add(publicationText);
//		textLable.add(new JLabel("发布时间:"));
//		textLable.add(cookieText);
		textLable.add(new JLabel("关键词excel文件名:"));
		textLable.add(excelText);
		textLable.add(new JLabel("数据库:"));
		textLable.add(dbSelect);
		textLable.add(new JLabel("搜索关键词范围: "));
		textLable.add(this.pubLabel);
		
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

	public JTextField getIntervalText() {
		return intervalText;
	}

	public void setIntervalText(JTextField intervalText) {
		this.intervalText = intervalText;
	}

	public JComboBox<String> getAccountSelect() {
		return accountSelect;
	}

	public void setAccountSelect(JComboBox<String> accountSelect) {
		this.accountSelect = accountSelect;
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

	public JTextField getFromYearText() {
		return fromYearText;
	}

	public void setFromYearText(JTextField fromYearText) {
		this.fromYearText = fromYearText;
	}

	public JTextField getToYearText() {
		return toYearText;
	}

	public void setToYearText(JTextField toYearText) {
		this.toYearText = toYearText;
	}
	
	
}