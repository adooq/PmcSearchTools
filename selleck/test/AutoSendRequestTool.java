package selleck.test;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class AutoSendRequestTool {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Robot robot;
		try {
			robot = new Robot();
			while(true){
				robot.keyPress(KeyEvent.VK_F5);
				robot.keyRelease(KeyEvent.VK_F5);
				Thread.sleep(15000);
			}
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
