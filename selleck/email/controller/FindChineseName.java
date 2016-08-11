package selleck.email.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class FindChineseName {
	// public static final String CHINESE_FIRST_NAME = "Ai,An,Ba,Bai,Bai,Ban,Bao,Bao,Bao,Bei,Ben,Bi,Bian,Bian,Bie,Bing,Bo,Bu,Bu,Cai,Cang,Cao,Cen,Chai,Chang,Chang,Che,Chen,Cheng,Cheng,Chi,Chong,Chu,Chu,Cong,Cui,Dai,Dang,Deng,Di,Diao,Ding,Dong,Dong,Dou,Dou,Du,Du,Du,Duan,Fan,Fan,Fang,Fang,Fei,Feng,Feng,Feng,Feng,Fu,Fu,Fu,Fu,Fu,Gan,Gan,Gao,Gao,Ge,Ge,Geng,Gong,Gong,Gong,Gong,Gu,Gu,Gu,Guan,Guang,Gui,Guo,Guo,Han,Hang,Hao,He,He,He,Heng,Hong,Hong,Hou,Hu,Hu,Hua,Hua,Hua,Huai,Huan,Huang,Hui,Huo,Ji,Ji,Ji,Ji,Ji,Ji,Ji,Ji,Ji,Ji,Ji,Jia,Jia,Jia,Jiang,Jiang,Jiang,Jiao,Jin,Jin,Jing,Jing,Jing,Ju,Kang,Ke/Kwa,Kong,Kou,Kuang,Kui,Lai,Lan,Lang,Lao,Lei,Li,Li,Li,Li,Lian,Lian,Liang,Liao,Lin,Lin,Ling,Liu,Liu,Long,Lou,Lu,Lu,Lu,Lu,Lu,Lu,Luan,Luo,Luo,Ma,Ma,Man,Mao,Mao,Mei,Meng,Meng,Mi,Mi,Mi,Miao,Miao,Min,Ming,Mo,Mu,Mu,Mu,Nai,Ni,Ning,Niu,Niu,Nong,Pan,Pang,Pang,Pei,Peng,Peng,Pi,Ping,Pu,Pu,Pu,Qi,Qi,Qi,Qian,Qiang,Qiao,Qin,Qiu,Qiu,Qiu,Qiu,Qu,Qu,Qu,Qu,Quan,Que,Ran,Ren,Rong,Rong,Rong,Ru,Ruan,Rui,Sang,Shan,Shan,Shang,Shao,Shao,Shen,Shen,Shen,Shen,Sheng,Shi,Shi,Shi,Shi,Shou,Shu,Shu,Shuang,Shui,Si,Song,Song,Su,Su,Sun,Suo,Tai,Tan,Tan,Tang,Tang,Tao,Teng,Tian,Tong,Tong,Tu,Wan,Wang,Wang,Wei,Wei,Wei,Wei,Wen,Wen,Wen,Weng,Wu,Wu,Wu,Wu,Wu,Wu,Xi,Xi,Xi,Xi,Xi,Xia,Xian,Xiang,Xiang,Xiao,Xie,Xie,Xing,Xing,Xiong,Xu,Xu,Xu,Xuan,Xue,Xun,Yan,Yan,Yan,Yan,Yan,Yang,Yang,Yang,Yao,Ye,Yi,Yi,Yi,Yin,Yin,Yin,Yin,Ying,Yong,You,Yu,Yu,Yu,Yu,Yu,Yu,Yu,Yu,Yu,Yu,Yu,Yuan,Yuan,Yue,Yun,Zai,Zan,Zang,Zhai,Zhan,Zhan,Zhang,Zhang,Zhao,Zhen,Zheng,Zhi,Zhong,Zhong,Zhong,Zhou,Zhu,Zhu,Zhu,Zhuang,Zhuo,Zong,Zou,Zu,Zuo,";
	public static final String REGEX = "(\\bAn\\b)|(\\bBa\\b)|(\\bBai\\b)|(\\bBan\\b)|(\\bBao\\b)|(\\bBei\\b)|(\\bBen\\b)|(\\bBi\\b)|(\\bBian\\b)|(\\bBie\\b)|(\\bBing\\b)|(\\bBo\\b)|(\\bBu\\b)|(\\bCai\\b)|(\\bCang\\b)|(\\bCao\\b)|(\\bCen\\b)|(\\bChai\\b)|(\\bChang\\b)|(\\bChe\\b)|(\\bChen\\b)|(\\bCheng\\b)|(\\bChi\\b)|(\\bChong\\b)|(\\bChu\\b)|(\\bCong\\b)|(\\bCui\\b)|(\\bDai\\b)|(\\bDang\\b)|(\\bDeng\\b)|(\\bDi\\b)|(\\bDiao\\b)|(\\bDing\\b)|(\\bDong\\b)|(\\bDou\\b)|(\\bDu\\b)|(\\bDuan\\b)|(\\bFan\\b)|(\\bFang\\b)|(\\bFei\\b)|(\\bFeng\\b)|(\\bFu\\b)|(\\bGan\\b)|(\\bGao\\b)|(\\bGe\\b)|(\\bGeng\\b)|(\\bGong\\b)|(\\bGu\\b)|(\\bGuan\\b)|(\\bGuang\\b)|(\\bGui\\b)|(\\bGuo\\b)|(\\bHan\\b)|(\\bHang\\b)|(\\bHao\\b)|(\\bHe\\b)|(\\bHeng\\b)|(\\bHong\\b)|(\\bHou\\b)|(\\bHu\\b)|(\\bHua\\b)|(\\bHuai\\b)|(\\bHuan\\b)|(\\bHuang\\b)|(\\bHui\\b)|(\\bHuo\\b)|(\\bJi\\b)|(\\bJia\\b)|(\\bJiang\\b)|(\\bJiao\\b)|(\\bJin\\b)|(\\bJing\\b)|(\\bJu\\b)|(\\bKang\\b)|(\\bKe/Kwa\\b)|(\\bKong\\b)|(\\bKou\\b)|(\\bKuang\\b)|(\\bKui\\b)|(\\bLai\\b)|(\\bLan\\b)|(\\bLang\\b)|(\\bLao\\b)|(\\bLei\\b)|(\\bLi\\b)|(\\bLian\\b)|(\\bLiang\\b)|(\\bLiao\\b)|(\\bLin\\b)|(\\bLing\\b)|(\\bLiu\\b)|(\\bLong\\b)|(\\bLou\\b)|(\\bLu\\b)|(\\bLuan\\b)|(\\bLuo\\b)|(\\bMa\\b)|(\\bMan\\b)|(\\bMao\\b)|(\\bMei\\b)|(\\bMeng\\b)|(\\bMi\\b)|(\\bMiao\\b)|(\\bMin\\b)|(\\bMing\\b)|(\\bMo\\b)|(\\bMu\\b)|(\\bNai\\b)|(\\bNi\\b)|(\\bNing\\b)|(\\bNiu\\b)|(\\bNong\\b)|(\\bPan\\b)|(\\bPang\\b)|(\\bPei\\b)|(\\bPeng\\b)|(\\bPi\\b)|(\\bPing\\b)|(\\bPu\\b)|(\\bQi\\b)|(\\bQian\\b)|(\\bQiang\\b)|(\\bQiao\\b)|(\\bQin\\b)|(\\bQiu\\b)|(\\bQu\\b)|(\\bQuan\\b)|(\\bQue\\b)|(\\bRan\\b)|(\\bRen\\b)|(\\bRong\\b)|(\\bRu\\b)|(\\bRuan\\b)|(\\bRui\\b)|(\\bSang\\b)|(\\bShan\\b)|(\\bShang\\b)|(\\bShao\\b)|(\\bShen\\b)|(\\bSheng\\b)|(\\bShi\\b)|(\\bShou\\b)|(\\bShu\\b)|(\\bShuang\\b)|(\\bShui\\b)|(\\bSi\\b)|(\\bSong\\b)|(\\bSu\\b)|(\\bSun\\b)|(\\bSuo\\b)|(\\bTai\\b)|(\\bTan\\b)|(\\bTang\\b)|(\\bTao\\b)|(\\bTeng\\b)|(\\bTian\\b)|(\\bTong\\b)|(\\bTu\\b)|(\\bWan\\b)|(\\bWang\\b)|(\\bWei\\b)|(\\bWen\\b)|(\\bWeng\\b)|(\\bWu\\b)|(\\bXi\\b)|(\\bXia\\b)|(\\bXian\\b)|(\\bXiang\\b)|(\\bXiao\\b)|(\\bXie\\b)|(\\bXing\\b)|(\\bXiong\\b)|(\\bXu\\b)|(\\bXuan\\b)|(\\bXue\\b)|(\\bXun\\b)|(\\bYan\\b)|(\\bYang\\b)|(\\bYao\\b)|(\\bYe\\b)|(\\bYi\\b)|(\\bYin\\b)|(\\bYing\\b)|(\\bYong\\b)|(\\bYou\\b)|(\\bYu\\b)|(\\bYuan\\b)|(\\bYue\\b)|(\\bYun\\b)|(\\bZai\\b)|(\\bZan\\b)|(\\bZang\\b)|(\\bZhai\\b)|(\\bZhan\\b)|(\\bZhang\\b)|(\\bZhao\\b)|(\\bZhen\\b)|(\\bZheng\\b)|(\\bZhi\\b)|(\\bZhong\\b)|(\\bZhou\\b)|(\\bZhu\\b)|(\\bZhuang\\b)|(\\bZhuo\\b)|(\\bZong\\b)|(\\bZou\\b)|(\\bZu\\b)|(\\bZuo\\b)";
	// public static Set<String> nameSet = new HashSet<String>();
	
	

	
	/**
	 * 读取originalFile， 找出全名是中文名字的，导出到rsFile
	 * 表头 fullName, email, address, organization
	 * @param args
	 */
	public static void main(String[] args) {
		Workbook wb;
		Workbook resultWb;
		File originalFile = new File("selleck_edm_author_nodup_usa.xlsx");
		File rsFile = new File("美国-中文姓名.xlsx");
		try {
			if(rsFile.exists()){
				rsFile.delete();
			}
			rsFile.createNewFile();
			
		    resultWb =  new XSSFWorkbook();
			Sheet rsSheet = resultWb.createSheet();
			Row rsRow = rsSheet.createRow(0);
			
			wb = WorkbookFactory.create(originalFile);
			Sheet sheet = wb.getSheet("selleck_edm_author_nodup_usa");
			int rowCount = sheet.getLastRowNum();
			int rowIndex = 1;
			Pattern p;
			Matcher matcher;
			String fullName;
			String email;
			String address;
			String organization;
			int i = 0; // 有无表头
			
			for (; i <= rowCount; i++) { // row从0开始
			// for (i = 1; i <= 1000; i++) { // row从0开始    for test
				try{
					Row row = sheet.getRow(i);
					fullName = row.getCell(0) == null ? "":row.getCell(0).getStringCellValue().trim();
					email =  row.getCell(1) == null ? "":row.getCell(1).getStringCellValue().trim();
//					address = row.getCell(2) == null ? "":row.getCell(2).getStringCellValue().trim();
//					organization = row.getCell(3) == null ? "":row.getCell(3).getStringCellValue().trim();
					// System.out.println(fullName);
						p = Pattern.compile(REGEX);
						matcher = p.matcher(fullName);
						if(matcher.find()){
							System.out.println(matcher.group()+"   ---  "+fullName);
							rsRow = rsSheet.createRow(rowIndex);
							rsRow.createCell(0).setCellValue(fullName); 
							rsRow.createCell(1).setCellValue(email);
//							rsRow.createCell(2).setCellValue(address);
//							rsRow.createCell(3).setCellValue(organization);
							rowIndex++;
	// 						break;
						
					}
				}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			}
			
			resultWb.write(new FileOutputStream(rsFile)); 
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
