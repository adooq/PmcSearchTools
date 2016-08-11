package selleck.email.update.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTextArea;

public class JTextArea4Log extends JTextArea {

	private static final long serialVersionUID = 6938649677737415856L;
	private  FileWriter fw = null;

	public JTextArea4Log() {
		super();
		if (fw == null) {
			try {
				File file = new File("log/log.txt");
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				fw = new FileWriter(file);
				System.out.println("创建日志文件成功!");
			} catch (IOException e) {
				System.out.println("创建日志文件失败！");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void append(String str) {
		str = new SimpleDateFormat("HH:mm:ss  yyyy-MM-dd").format(new Date()) + "    " + str;
		super.append(str);
		try {
			fw.write(str + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closefw(){
		try {
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
