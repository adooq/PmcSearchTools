package selleck.email.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import common.handle.frame.CreateFrame;

public class FrameMain {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final CreateFrame frame = CreateFrame.getFrame();
		final catechEmailContorller contorller = new catechEmailContorller();
		frame.getLoggerTA().setText("开始：取配置文件邮箱邮件信息\n" +
									"停止：停止当前操作\n");
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				contorller.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				contorller.setStartFlag(false);
			}
		});		
		
		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					boolean StartFlag = contorller.isStartFlag();
					while(!StartFlag){
						try {Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						StartFlag = contorller.isStartFlag();
					}
					String ret = "";
					String select = frame.getMoveCB().getSelectedItem().toString();
					if(select.equals("抓数据")){		
						ret = contorller.pickAuthorEmail(frame);
						if(ret==null){
							frame.getLoggerTA().append("连接数据库异常\n");
						}
					}else if(select.equals("只抓lab url邮箱")){
						frame.getLoggerTA().append("只抓lab url邮箱\n");
						ret = contorller.pickLabEmail(frame);
						System.out.println("pickLabEmail finished");
						if(ret==null){
							frame.getLoggerTA().append("连接数据库异常3\n");
						}
					}
					contorller.setStartFlag(false);
					frame.stopMouseClicked();
				}
			}
		});
		gmailThread.start();
	}

}
