package selleck.email.controller;

import java.util.List;

import common.handle.frame.CreateFrame;
import common.handle.model.Criteria;
import selleck.email.pojo.LabmemReference;
import selleck.email.service.IOrganitionEmailService;
import selleck.email.service.impl.OrganitionEmailServiceImpl;
import selleck.email.update.tools.JTextArea4Log;

public class catechEmailContorller {
	private boolean startFlag = false;// 本线程开始标志
	private IOrganitionEmailService mapper = new OrganitionEmailServiceImpl();

	public boolean isStartFlag() {
		return startFlag;
	}

	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}

	public String pickAuthorEmail(CreateFrame frame) {
		JTextArea4Log logger = frame.getLoggerTA();
		logger.append("开始搜索Email\n");
		EmailPick pick = new EmailPick();
		Criteria criteria = new Criteria();
		List<LabmemReference> list = mapper.selectByExample(criteria);
		// LabmemReference email = new LabmemReference();
		// pick.exactEmailList("https://www.bcm.edu/departments/pharmacology/barthlab/people","123",email);
		for (LabmemReference email : list) {
			if (email.getOrganitURL() != null && email.getBossURL() == null
					&& email.getFacultyURL() == null
					&& email.getLabURL() == null) {
				pick.allEmailList(email.getOrganitURL(),
						email.getOrganitName(), email);
			} else {
				if (email.getBossURL() != null) {
					pick.catchBossEmail(email);
				}
				if (email.getLabURL() != null) {
					pick.exactEmailList(email.getLabURL(), email.getBossName(),
							email);
				}
				if (email.getFacultyURL() != null) {
					pick.exactEmailList(email.getFacultyURL(),
							email.getBossName(), email);
				}
			}
		}
		/*
		 * while(startFlag){
		 * 
		 * }
		 */
		logger.append("完成本组数据的搜索进行数据库数据的保存\n");

		return "";
	}

	/**
	 * 只抓取lab_url里的邮箱
	 * 
	 * @param frame
	 * @return
	 */
	public String pickLabEmail(CreateFrame frame) {
		JTextArea4Log logger = frame.getLoggerTA();
		logger.append("开始搜索 lab_url Email\n");
		EmailPick pick = new EmailPick();
		Criteria criteria = new Criteria();
		List<LabmemReference> list = mapper.selectLabUrlNotNull(criteria);
		// LabmemReference email = new LabmemReference();
		// pick.exactEmailList("https://www.bcm.edu/departments/pharmacology/barthlab/people","123",email);
		int count = 0;
		for (LabmemReference email : list) {
			count ++;
			logger.append("\n======= email count "+ count + "\n");
			if (email.getLabURL() != null && !email.getLabURL().equals("") && count >= 823) {
				pick.exactEmailList(email.getLabURL(), email.getBossName(),email);
			}
		}
		/*
		 * while(startFlag){
		 * 
		 * }
		 */
		logger.append("完成搜索\n");

		return "";
	}

}
