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

@SuppressWarnings("serial")
public class UpdatePMCFrame extends FrameClass{
	protected JTextField excelText; // excel 文件名 label
	protected JTextField intervalText; // 采集时间间隔  label
	protected JTextField pubLabel; // 用来填写期刊excel里需要抓的范围，形如:  201-400
	
	private UpdatePMCFrame(){
		initControls();
		initLayout();
	}
	
	public static final UpdatePMCFrame getFrame(){
		UpdatePMCFrame frame = new UpdatePMCFrame();
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
		setTitle("自动更新PMC文章");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(new ImageIcon("icon.png").getImage());
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				btnExitMouseClicked();
			}
		});
		
		moveLB = new JLabel("操作:");
		startBT = new JButton("开始");
		stopBT = new JButton("停止");
		stopBT.setEnabled(false);
		
		moveCB = new JComboBox<String>(new String[]{"自动更新PMC文章"});
		
		loggerTA = new JTextArea4Log();
		loggerTA.setEditable(false);
		loggerTA.setAutoscrolls(true);
		loggerTA.setLineWrap(true); 
		loggerTA.invalidate();
		
		pubLabel = new JTextField("1-100", 10);
		
		loggerSP = new JScrollPane();
		loggerSP.setPreferredSize(new Dimension(800, 600));
		loggerSP.setAutoscrolls(true);
		loggerSP.setViewportView(loggerTA);
		
		publicationText = new JTextField("2015/01/01", 10); // 把publicationText当做startEntrez用
		cookieText =  new JTextField("2015/03/31",10); // 把cookieText当做endEntrez用
		excelText =  new JTextField("pmc publications.xlsx",15); // excel 文件名 label
		intervalText =  new JTextField("5",3); // 间隔时间 label
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
		pLable.add(new JLabel("发布时间:"));
		pLable.add(publicationText);
		pLable.add(new JLabel("发布时间:"));
		pLable.add(cookieText);
		
		JPanel textLable = new JPanel(new FlowLayout(FlowLayout.LEFT));
		textLable.add(new JLabel("期刊excel文件名:"));
		textLable.add(excelText);
		textLable.add(new JLabel("间隔（秒）:"));
		textLable.add(intervalText);
		textLable.add(new JLabel("搜索期刊范围: "));
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

	public JTextField getPubLabel() {
		return pubLabel;
	}

	public void setPubLabel(JTextField pubLabel) {
		this.pubLabel = pubLabel;
	}
	
	
}
