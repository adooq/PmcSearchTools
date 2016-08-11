package selleck.email.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import common.handle.frame.DownloadCNKIpdfFrame;

public class DownloadCNKIpdfMain {
	 public final static int BUFFER = 1024;
	 public static final String PDF_DIR = "z:\\cnki\\CNKI pdf\\";
	 public static final String WANGGUO_PDF_DIR = "z:\\cnki\\CNKI pdf for wangguo\\";
	 public static final int timeInterval = 121000; // 下载60秒频率太高，2小时左右后ip会被封下载
	 public static final boolean isForWangguo = false; // 是否是给王果用的cnki pdf downloader，用的是search_cnki_by_publication_for_wangguo表
	 
	public static void main(String[] args) {
		final DownloadCNKIpdfFrame frame = DownloadCNKIpdfFrame.getFrame();
		final DownloadCNKIpdfController controller = new DownloadCNKIpdfController();

		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				controller.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				controller.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = controller.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = controller.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					if (select.equals("下载 CNKI pdf")) {
						if(isForWangguo){
							controller.downloadCNKIpdfForWangguo(frame.getPublicationText().getText().trim(), frame.getCookieText().getText().trim(),frame.getLoggerTA());
						}else{
							controller.downloadCNKIpdf(frame.getPublicationText().getText().trim(), frame.getCookieText().getText().trim(),frame.getLoggerTA());
						}
					}
					controller.setStartFlag(false);
					frame.stopMouseClicked();
				}
			}
		});
		gmailThread.start();
	}

}
