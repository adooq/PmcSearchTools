package selleck.email.update.tools;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JTextArea;

public class HMARobot {
	static String recentIP;// HMA的内网IP，不是外网IP。换IP时该IP一般也会变，也有可能不变。
	static int count;// 计数，recentIP三次不变，说明出现了弹窗，需要按Enter。
	static boolean isSuccessed;

	static {
		isSuccessed = true;
		count = 0;
		try {
			recentIP = InetAddress.getLocalHost().getHostAddress().toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void changeIP(JTextArea logger) {
		logger.append("      HMA change IP     \n");
		changeIP(); 
	}

	public void changeIP() {
		Dimension dm = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int) dm.getWidth();
		int height = (int) dm.getHeight();
		int minWidth = 90;
		int minHeight = 455;
		int moveWidth = (int) (width - minWidth);
		int moveHeight = (int) (height - minHeight);
		try {
			Robot robot = new Robot();
			robot.mouseMove(moveWidth, moveHeight);
			clickMouse(robot, InputEvent.BUTTON1_MASK);
			if (!isSuccessed) {
				inputKey(robot, Event.ENTER);
			}
			robot.delay(60000);
			isSuccessed = checkIP();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean checkIP() throws UnknownHostException {
		String currentIP = InetAddress.getLocalHost().getHostAddress().toString();
		if (!currentIP.startsWith("192.168") && currentIP.equals(recentIP)) {
			count++;
			if (count == 3) {
				count = 0;
				return false;
			}
		} else {
			recentIP = currentIP;
			count = 0;
		}
		return true;
	}

	public void inputKey(Robot robot, int button) {
		robot.keyPress(button);
		robot.keyRelease(button);
	}

	public void clickMouse(Robot robot, int button) {
		robot.mousePress(button);
		robot.mouseRelease(button);
	}
}
