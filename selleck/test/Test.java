package selleck.test;

import java.io.IOException;

public class Test {
	public static final String[] TITLES = {"RM","BSN","MSN","MD","RN","Associate Professor","Assistant Professor","MS","PhD","MSc","DDS","Professor",
		"MSD","Dentist","FRCP","FRCPCH","FRCPC","RPh","FACC","FAHA","MPH","MBA","MDS","BDS","EdD","MSW","DACVECC","DVM","MRCP","Vice President",
		"DACVIM","DACVP","MRCVS","BVetMed","DACVAA","FACS","Director","Editor","Issue Editor","Guest Editor","Research Scientist","Dr","Prof","MBBS",
		"BSc","BM","MA","FRCPA","FRACP","DO","DSc","DM","MRCPsych","Coordinator","secretary","President","Master","for"}; // 人名里的头衔
	public static void main(String[] args) throws IOException {
		
		// 如果是以头衔开头，把头衔去掉
		StringBuffer titleSB = new StringBuffer("(?i)"); // (?i)表示正则大小写不敏感
		for (String t : TITLES) {
			titleSB.append("(\\b");
			char[] chars = t.toCharArray();
			for (char c : chars) {
				titleSB.append(c).append("\\.?");
			}
			titleSB.delete(titleSB.length() - 3, titleSB.length()); // 去掉最后的\.?
			titleSB.append("(\\.|\\b))").append("|");
		}
		titleSB.deleteCharAt(titleSB.length() - 1); // 去掉最后一个|
		System.out.println(titleSB);

		String target = "R.M.Hickey";
		String[] names = target.split(titleSB.toString());
		// target = target.replaceAll("(?i)\\A(Professor)|(MD)", "");
		System.out.println(names.length);
		// System.out.println(names[1].trim().isEmpty() + "|");
		for (String n : names) {
			System.out.println(n + "|");
		}
	}

}
