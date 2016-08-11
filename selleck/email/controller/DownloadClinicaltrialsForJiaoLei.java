package selleck.email.controller;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * 自动从一个关键词excel表，到http://clinicaltrials.gov/ct2/results?term=abt-263&show_down=Y上下载相关文章
 * @author fscai
 *
 */
public class DownloadClinicaltrialsForJiaoLei {
	public static final String DOWNLOAD_DIR = "";
	
	// for test
	public static void main(String[] args) throws Exception{
		List<String[]> catAndpname = new ArrayList<String[]>();
		String[] ss = {"S8054","NCT01856101|BEZ235"};
		catAndpname.add(ss);
//		String[] ss1 = {"S8056","Lomeguatrib"};
//		catAndpname.add(ss1);
//		String[] ss2 = {"S8057","Pacritinib|SB1518|SB 1518|SB-1518"};
//		catAndpname.add(ss2);
//		String[] ss3 = {"S7505","(S)-crizotinib"};
//		catAndpname.add(ss3);
//		String[] ss4 = {"S8059","Nutlin-3a|Nutlin3a|Nutlin 3a|(-)-Nutlin-3"};
//		catAndpname.add(ss4);
		List<ClinicStudy> csList = downloadStudies(catAndpname);
		for(ClinicStudy cs : csList){
			System.out.println(cs.toString());
			String sponor = new String(cs.getSponsorAndCollaborators().getBytes(),"utf-8");
			System.out.println("sponor: "+sponor);
		}
		
// 		downloadStudies();
		
	}
	
	/** 根据产品名的data.xlsx，到http://clinicaltrials.gov/ct2/results?term=abt-263&show_down=Y上下载相关文章，并合并到final.xlsx
	 * 操作步骤：
	1. 准备数据文件。文件名为data.xlsx；第四列为关键词，多个关键词用|号分隔；数据第二行开始，第一行为表头。
	2. 把Download.jar 和 data.xlsx 放在同一文件夹下，清除目录下所有其他文件。
	3. 双击即开始下载，下载过程中会产生每个关键词的zip文件和解压出来的csv文件，都是过程文件，一般不需要。最终生成一个final.csv。
	4. 打开final.csv，更改日期列格式。"设置单元格格式"--自定义，输入yyyy-mm（源文件日期格式为yyyy-mm，excel自作聪明地修改了日期格式，只能这样手动修改，fucking Microsoft）。
	5. 把final.csv另存为excel文件(xlsx)
	 * @param args
	 */
	public static void downloadStudies() {
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(new File(DOWNLOAD_DIR + "data.xlsx"));
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheet("Sheet1");
		int rowCount = sheet.getLastRowNum();
		System.out.println("rowCount:" + rowCount);
		for (int i = 1801; i <= rowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
			try {
				Row row = sheet.getRow(i);
				String cat = row.getCell(0).getStringCellValue().trim(); // cat号(S号)
				String singleName = row.getCell(1).getStringCellValue().trim(); // 产品单名
				String productNames = row.getCell(3).getStringCellValue().trim(); // 第四列是产品名
				String[] productArr = productNames.split("\\|");
				if (productArr.length > 0) {
					StringBuffer productKeyword = new StringBuffer(); // 形如 "(-)-Nutlin-3"+OR+"(+)-Nutlin-3" ，每个关键词加引号，可用+OR+组合多个关键词
					productKeyword.append(URLEncoder.encode("\""+ productArr[0].trim() + "\"", "utf-8"));
					for (int j = 1; j < productArr.length; j++) {
						productKeyword.append("+OR+").append(URLEncoder.encode("\"" + productArr[j].trim() + "\"", "utf-8"));
					}

					downloadByKeyword(productKeyword.toString(),String.valueOf(i));
					unZipDownloadedFile(String.valueOf(i));
					parseDowloadedExcels2CSV(productNames, String.valueOf(i),cat, singleName);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	
	/**
	 * 给后台程序调用。根据传入的catAndpname（cat号和产品名），
	 * 到http://clinicaltrials.gov/ct2/results?term=abt-263&show_down=Y上下载相关文章，
	 * 返回所有文章信息ClinicStudy。
	 * @param catAndpname 是个String数组，String[0]是cat号，String[1]是产品名的各种形式，用|分隔
	 * @return List<ClinicStudy>
	 */
	public static List<ClinicStudy> downloadStudies(List<String[]> catAndpname) {
		List<ClinicStudy> studies = new ArrayList<ClinicStudy>();
		
		for (Iterator<String[]> iter = catAndpname.iterator(); iter.hasNext();) {
			String[] arr = iter.next();
			String cat = arr[0];
			String productNames = arr[1];
			String[] productArr = productNames.split("\\|");
			if (productArr.length > 0) {
				StringBuffer productKeyword = new StringBuffer(); // 形如 "(-)-Nutlin-3"+OR+"(+)-Nutlin-3" ，每个关键词加引号，可用+OR+组合多个关键词
				try {
					productKeyword.append(URLEncoder.encode("\""+ productArr[0].trim() + "\"", "utf-8"));
					for (int j = 1; j < productArr.length; j++) {
						productKeyword.append("+OR+").append(URLEncoder.encode("\"" + productArr[j].trim()+ "\"", "utf-8"));
					}
					downloadByKeyword(productKeyword.toString(), cat);
					unZipDownloadedFile(cat);
					studies.addAll(parseDowloadedExcels(productNames, cat, cat));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch(Exception ee){
					ee.printStackTrace();
				}
			}

		}
		return studies;
	}
	
	
	/**
	 * 根据关键词，去clinicaltrials.gov下载study
	 * @param keyword  形如 "(-)-Nutlin-3"+OR+"(+)-Nutlin-3" ，每个关键词加引号，可用+OR+组合多个关键词
	 * @param fileName 下载的文件名 , fileName + .zip
	 */
	private static void downloadByKeyword(String keyword , String fileName){
		// http://clinicaltrials.gov/ct2/results/download?down_stds=all&down_typ=fields&down_flds=all&down_fmt=csv&term=ABT-263+OR+Navitoclax&show_down=Y
		String url = "http://clinicaltrials.gov/ct2/results/download?down_stds=all&down_typ=fields&down_flds=all&down_fmt=csv&term="+keyword+"&show_down=Y";
		File file = new File(DOWNLOAD_DIR+fileName+".zip");
		if(file.exists()){
			file.delete();
		}
		try {
			file.createNewFile();
			HTTPUtils.downloadFile(url, null, HTTPUtils.GET, null, file);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * 解压文件。下载下来的文件名是keyword.zip，里面被压缩的文件都叫study_fields.csv，所以要解压并且把解压的文件重命名为keyword.csv
	 * @param fileName 下载的文件名 , fileName + .zip
	 */
	private static void unZipDownloadedFile(String fileName){
		FileOutputStream fileOut = null;
        File file; 
        ZipInputStream zis = null;
        try{ 
        	zis = new ZipInputStream (new BufferedInputStream(new FileInputStream(DOWNLOAD_DIR + fileName + ".zip"))); 
        	ZipEntry zipEntry = null; 
            if((zipEntry = zis.getNextEntry()) != null){ // ZipEntry代表压缩文件中的每一个文件。这里压缩中只有一个文件，所以用if。
                file = new File(DOWNLOAD_DIR + fileName + ".csv"); 
                if(file.exists()){
                	file.delete();
                }
                file.createNewFile();
                
                if(zipEntry.isDirectory()){ 
                    file.mkdirs(); 
                } else{
                    fileOut = new FileOutputStream(file);
                    int readedBytes;
                    byte[] buf = new byte[1024]; 
                    while((readedBytes = zis.read(buf) ) > 0){ 
                        fileOut.write(buf , 0 , readedBytes); 
                    } 
                }  
            } 
        }catch(IOException ioe){ 
            ioe.printStackTrace(); 
        }finally{
        	if(fileOut != null){
        		try {
					fileOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        	if(zis != null){
        		try {
					zis.closeEntry();
					zis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}     
        	}
        }
	}
	
	/**
	 * 先在G列进行关键词搜索，如果用于搜索的所有关键词都不出现在G列中，那么这行数据删除。
		取7列信息，6列用于展示，1列URL用于添加链接。6列的顺序分别为：B,D,F,H,R,K。其中R列的日期展示形式要变一下：比如，下载下来的是Apr-10 ，要转换成2010-04。
		最后，增加一列S号信息
		然后合并所有产品的excel
		@param keyword  形如  Nutlin-3b|Nutlin 3b|Nutlin3b|(+)-Nutlin-3 ，竖线分隔
		@param fileName 下载的文件名 , fileName + .csv
		@param cat cat号（S号）
		@param productName 产品正式名
	 */
	private static void parseDowloadedExcels2CSV(String keyword , String fileName , String cat , String productName) {
		String[] keywords = keyword.split("\\|");
		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			File csv = new File(DOWNLOAD_DIR + fileName + ".csv"); // CSV文件
			if (!csv.exists()) {
				return;
			}
			File finalCSV = new File(DOWNLOAD_DIR + "final.csv"); // 最终输出CSV文件
			if (!finalCSV.exists()) {
				finalCSV.createNewFile();
			}

			br = new BufferedReader(new FileReader(csv));
			bw = new BufferedWriter(new FileWriter(finalCSV, true));
			// 读取直到最后一行
			String line = "";
			while ((line = br.readLine()) != null) {
				System.out.println("parse excel line: " + line);
				// 把一行数据分割成多个字段
				String[] columns = line.split("\",\""); // 以 "," 分列
				columns[0] = columns[0].replaceFirst("\\d+,\"", ""); // 第一列形如： 1,"NCT02070094" ，除去第一个引号前的部分
				columns[24] = columns[24].replaceFirst("\"", "");// 除去最后一列的最后个引号
				String gColumn = columns[5]; // G列
				for (String key : keywords) {
					key = key.trim();
					if (gColumn.toLowerCase().contains(key.toLowerCase())) {
						bw.newLine();
						// B,D,F,H,R,K,url 列
						String dateStr = changeDateFormat(columns[16]);
						String newLine = "\"" +cat + "\",\"" +productName + "\",\"" + columns[0] + "\",\""
								+ columns[2] + "\",\"" + columns[4] + "\",\""
								+ columns[6] + "\",\"" + dateStr + "\",\""
								+ columns[9] + "\",\"" + columns[24] + "\"";
						System.out.println(newLine);
						bw.write(newLine);
						break;
					}
				}

				System.out.println();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 先在下载的excel文件的G列进行关键词搜索，如果用于搜索的所有关键词都不出现在G列中，那么这行数据删除。
		取7列信息，6列用于展示，1列URL用于添加链接。6列的顺序分别为：B,D,F,H,K,R。
		最后，增加S号(cat号)信息
		@param keyword  形如  Nutlin-3b|Nutlin 3b|Nutlin3b|(+)-Nutlin-3 ，竖线分隔
		@param fileName 下载的文件名 , fileName + .csv
		@return  List<ClinicStudy>
	 */
	private static List<ClinicStudy> parseDowloadedExcels(String keyword , String fileName , String cat) {
		List<ClinicStudy> rs = new ArrayList<ClinicStudy>();
		String[] keywords = keyword.split("\\|");
		BufferedReader br = null;
		try {
			File csv = new File(DOWNLOAD_DIR + fileName + ".csv"); // CSV文件
			if (!csv.exists()) {
				return rs;
			}
			
			/* 显式地指定下编码，FileReader默认编码不保险
			 * FileReader ： Convenience class for reading character files. 
			 * The constructors of this class assume that the default character encoding and the default byte-buffer size are appropriate. 
			 * To specify these values yourself, construct an InputStreamReader on a FileInputStream.
			FileReader fr = new FileReader(csv);
			System.out.println("FileReader encoding: "+fr.getEncoding());
			br = new BufferedReader(fr);
			*/
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(csv),"utf-8"));

			String line = "";
			while ((line = br.readLine()) != null) {
				// 把一行数据分割成多个字段
				String[] columns = line.split("\",\""); // 以 "," 分列
				columns[0] = columns[0].replaceFirst("\\d+,\"", ""); // 第一列形如： 1,"NCT02070094" ，除去第一个引号前的部分
				columns[24] = columns[24].replaceFirst("\"", "");// 除去最后一列的最后个引号
				String gColumn = columns[5]; // G列
				for (String key : keywords) {
					key = key.trim();
					if (gColumn.toLowerCase().contains(key.toLowerCase())) {
						ClinicStudy cs = new ClinicStudy();
						cs.setCat(cat);
						cs.setNctNumber(columns[0]);
						cs.setRecruitment(columns[2]);
						cs.setConditions(columns[4]);
						cs.setSponsorAndCollaborators(columns[6]);
						cs.setPhase(columns[9]);
						cs.setStartDate(columns[16]);
						cs.setUrl(columns[24]);
						rs.add(cs);
						break;
					}
				}
			}
			return rs;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return rs;
		} catch (IOException e) {
			e.printStackTrace();
			return rs;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 删除下载的zip文件和解压出来的csv文件
			File csv = new File(DOWNLOAD_DIR + fileName + ".csv"); // CSV文件
			if(csv.exists()){
				csv.delete();
			}
			File zip = new File(DOWNLOAD_DIR + fileName + ".zip"); // ZIP文件
			if(zip.exists()){
				zip.delete();
			}
			
		}
	}
	
	/**
	 * April 2011  转成 2011-04
	 * @param src
	 * @return
	 */
	private static String changeDateFormat(String src){
		if(src.contains("January")){
			return src.replaceAll("January ", "") + "-01";
		}else if(src.contains("February")){
			return src.replaceAll("February ", "") + "-02";
		}else if(src.contains("March")){
			return src.replaceAll("March ", "") + "-03";
		}else if(src.contains("April")){
			return src.replaceAll("April ", "") + "-04";
		}else if(src.contains("May")){
			return src.replaceAll("May ", "") + "-05";
		}else if(src.contains("June")){
			return src.replaceAll("June ", "") + "-06";
		}else if(src.contains("July")){
			return src.replaceAll("July ", "") + "-07";
		}else if(src.contains("August")){
			return src.replaceAll("August ", "") + "-08";
		}else if(src.contains("September")){
			return src.replaceAll("September ", "") + "-09";
		}else if(src.contains("October")){
			return src.replaceAll("October ", "") + "-10";
		}else if(src.contains("November")){
			return src.replaceAll("November ", "") + "-11";
		}else if(src.contains("December")){
			return src.replaceAll("December ", "") + "-12";
		}else{
			return "";
		}
	}

}

class ClinicStudy{
	private int productId;
	private String cat;
	private String nctNumber;
	private String recruitment;
	private String conditions;
	private String sponsorAndCollaborators;
	private String phase;
	private String startDate; //  形如： Aug-14
	private String url;

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("cat: ").append(cat).append("\n");
		sb.append("nctNumber: ").append(nctNumber).append("\n");
		sb.append("recruitment: ").append(recruitment).append("\n");
		sb.append("conditions: ").append(conditions).append("\n");
		sb.append("sponsorAndCollaborators: ").append(sponsorAndCollaborators).append("\n");
		sb.append("phase: ").append(phase).append("\n");
		sb.append("startDate: ").append(startDate).append("\n");
		sb.append("url: ").append(url).append("\n");
		return sb.toString();
	}
	
	public int getProductId() {
		return productId;
	}
	public void setProductId(int productId) {
		this.productId = productId;
	}
	public String getCat() {
		return cat;
	}
	public void setCat(String cat) {
		this.cat = cat;
	}
	public String getNctNumber() {
		return nctNumber;
	}
	public void setNctNumber(String nctNumber) {
		this.nctNumber = nctNumber;
	}
	public String getRecruitment() {
		return recruitment;
	}
	public void setRecruitment(String recruitment) {
		this.recruitment = recruitment;
	}
	public String getConditions() {
		return conditions;
	}
	public void setConditions(String conditions) {
		this.conditions = conditions;
	}
	public String getSponsorAndCollaborators() {
		return sponsorAndCollaborators;
	}
	public void setSponsorAndCollaborators(String sponsorAndCollaborators) {
		this.sponsorAndCollaborators = sponsorAndCollaborators;
	}
	public String getPhase() {
		return phase;
	}
	public void setPhase(String phase) {
		this.phase = phase;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	
}

/**
 * http 工具类
 * 
 * @author fscai
 * 
 */
class HTTPUtils {
	public static final String GET = "GET";
	public static final String POST = "POST";


	/**
	 * 访问一个网址，返回最终url cookie html 信息，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookie , 如果没有则填null
	 * @param method  get or post
	 * @param params  body参数，如果是GET,则为null
	 * @return  Map<String,String> 有三个key，返回url  , cookie , html
	 */
	public static Map<String, String> getCookieUrlAndHtml(String targetURL,String cookie, String method, Map<String, String> params) {
		Map<String, String> rs = new HashMap<String, String>();
		HttpClientContext httpClientContext = sendRequest(targetURL, cookie,method, params);
		if(httpClientContext == null){ // 请求失败
			return rs;
		}
		HttpEntity entity = httpClientContext.getResponse().getEntity();
		List<Cookie> newCookiesList = httpClientContext.getCookieStore().getCookies();
		String newCookies = convertCookieList2Str(newCookiesList);
		newCookies = updateCookieStr(cookie, newCookies);
		try {
			if (entity != null && entity.getContentType().toString().contains("html")) {
				byte[] contentArray = EntityUtils.toByteArray(entity);
				if (contentArray.length > 0) {
					String contentType = entity.getContentType().toString();
					int charsetIndex = contentType.indexOf("charset=");
					String encoding = charsetIndex > -1 ? contentType.substring(charsetIndex + 8) : null;
					// System.out.println("encoding: "+encoding);
					String htmlStr = new String(contentArray,encoding == null ? "iso-8859-1" : encoding);
					rs.put("html", htmlStr);
				}
			}

			rs.put("cookie", newCookies);
			rs.put("url", targetURL);
			return rs;

		} catch (IOException e) {
			System.out.println("  EntityUtils.consume(entity)  IOException");
			// logger.append("  EntityUtils.consume(entity)  IOException" +
			// "\n");
			// e.printStackTrace();
		} finally {
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return rs;
	}
	
	/**
	 * 访问一个网址，下载文件，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookie , 如果没有则填null
	 * @param method  get or post
	 * @param params  body参数，如果是GET,则为null
	 * @param file , 要下载的目标文件，主要用来传入file的文件名路径。
	 */
	public static void downloadFile(String targetURL, String cookie, String method, Map<String, String> params , File file){
		HttpClientContext httpClientContext = sendRequest(targetURL, cookie, method, params);
		if(httpClientContext != null){
			HttpEntity httpEntity = httpClientContext.getResponse().getEntity();
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file);
				InputStream input = httpEntity.getContent();
				byte b[] = new byte[1024];
				int j = 0;
				while( (j = input.read(b))!=-1){
					output.write(b,0,j);  
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(output != null){
					try {
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					EntityUtils.consume(httpEntity);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * 访问一个网址，返回最终HttpClientContext信息，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookie , 如果没有则填null
	 * @param method  get or post
	 * @param params  body参数，如果是GET,则为null
	 * @return  HttpClientContext，如果访问失败，return null
	 */
	public static HttpClientContext sendRequest(String targetURL, String cookie, String method, Map<String, String> params) {
		CloseableHttpClient httpclient = null;
		HttpClientContext httpClientContext = HttpClientContext.create();
		httpclient = HttpClientBuilder.create().build();
		
		HttpRequestBase httpRequest;
		if (method.equals(GET)) {
			httpRequest = new HttpGet(targetURL);
		} else if (method.equals(POST)) {
			httpRequest = new HttpPost(targetURL);
			
			if (params != null) {
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				Set<String> keySet = params.keySet();
				for (String key : keySet) {
					nvps.add(new BasicNameValuePair(key, params.get(key)));
				}
				try {
					((HttpPost)httpRequest).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			
		} else {
			return null;
		}

		
		if(cookie != null){
			httpRequest.setHeader("Cookie", cookie.replaceAll(";", ""));
		}
		// httpRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");

		
		HttpResponse response = null;
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(300000).setConnectTimeout(300000)
				.setSocketTimeout(300000).build();
		httpRequest.setConfig(requestConfig);
		try {
			response = httpclient.execute(httpRequest, httpClientContext);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
			// logger.append(" httpclient.execute(httpGet) ClientProtocolException\n");
			return null;
		}catch (javax.net.ssl.SSLHandshakeException ssle){
			// https ssl安全验证失败，但仍可访问
			ssle.printStackTrace();
			return null;
		} catch (IOException e1) {
			System.out.println("httpclient.execute(httpGet) IOException");
			e1.printStackTrace();
			return null;
		}catch (Exception ee){
			ee.printStackTrace();
			return null;
		}


		List<Cookie> newCookiesList = httpClientContext.getCookieStore().getCookies();
		String newCookies = convertCookieList2Str(newCookiesList);
		newCookies = updateCookieStr(cookie , newCookies);
		// System.out.println("new cookies: "+newCookies);
		
		if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
			return httpClientContext;
		// if(true){
			// 请求成功 取得请求内容
//			HttpEntity entity = response.getEntity();
//			
//			
//			try {
//				if (entity != null && entity.getContentType().toString().contains("html")) {
//					byte[] contentArray = EntityUtils.toByteArray(entity);
//					if (contentArray.length > 0) {
//						String contentType = entity.getContentType().toString();
//						int charsetIndex = contentType.indexOf("charset=");
//						String encoding = charsetIndex > -1 ? contentType.substring(charsetIndex + 8) : null;
//						// System.out.println("encoding: "+encoding);
//						String htmlStr = new String(contentArray, encoding == null ? "iso-8859-1" : encoding);
//						rs.put("html", htmlStr);
//					}
//				}
//				
//				rs.put("cookie", newCookies);
//				rs.put("url", targetURL);
//				return entity;
				
//			} catch (IOException e) {
//				System.out.println("  EntityUtils.consume(entity)  IOException");
//				// logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
//				// e.printStackTrace();
//			}finally{
//				try {
//					EntityUtils.consume(entity);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
			
			
		}
		else if (HttpStatus.SC_MOVED_TEMPORARILY == response.getStatusLine().getStatusCode()) { // 302 跳转
			// 请求成功 取得请求内容
			HttpEntity entity = response.getEntity();

			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				System.out.println("  EntityUtils.consume(entity)  IOException");
				// logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
				// e.printStackTrace();
			}
						
			Header locationHeader = response.getFirstHeader("location");
			if (locationHeader != null) {
				// getCookie(String targetURL , String cookie , String method ,
				// Map<String, String> params)
				// System.out.println("302: "+locationHeader.getValue());
				return sendRequest(locationHeader.getValue(), newCookies, HTTPUtils.GET, null);
			}
		}
		else {
			HttpEntity entity = response.getEntity();

			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				System.out.println("  EntityUtils.consume(entity)  IOException");
				// logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
				// e.printStackTrace();
			}
			System.out.println("  访问失败 " + response.getStatusLine().getStatusCode());
			// logger.append("  下载请求失败" + "\n");
		}

		return null;
	}
	
	/**
	 * 把List<Cookie> 转换成String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * @param cookies  List<Cookie>
	 * @return 如果cookies为null 或是空集合，返回""
	 */
	public static String convertCookieList2Str(List<Cookie> cookies){
		if(cookies == null){
			return "";
		}
		StringBuffer cookieStr = new StringBuffer();
		for (Cookie c : cookies) {
			cookieStr.append(c.getName()).append("=").append(c.getValue()).append(";");
		}
		return cookieStr.toString();
	}
	
	
	
	/**
	 * 更新cookie list。如果新的cookie list不包含老的list中的某个cookie,加入到新的list
	 * @param oldCookies
	 * @param newCookies
	 */
	private static List<Cookie> updateCookieList(List<Cookie> oldCookies , List<Cookie> newCookies){
		if(oldCookies == null){
			return newCookies;
		}
		if(newCookies == null){
			return oldCookies;
		}
		for(Cookie oc : oldCookies){
			boolean notInNew = true;
			for(Cookie nc : newCookies){
				if(nc.getName().equals(oc.getName())){
					notInNew = false;
					break;
				}
			}
			if(notInNew){
				newCookies.add(oc);
			}
		}
		return newCookies;
	}
	
	/**
	 * 更新cookie list，即如果新的cookie list不包含老的list中的某个cookie,加入到新的list
	 * @param oldCookies  String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * @param newCookies  String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * @return 新的cookie String
	 */
	public static String updateCookieStr(String oldCookies ,String newCookies){
		if(oldCookies == null || oldCookies.trim().isEmpty()){
			return newCookies;
		}
		if(newCookies == null|| newCookies.trim().isEmpty()){
			return oldCookies;
		}
		
		String[] oldCookieArr = oldCookies.split(";");
		String[] newCookieArr = newCookies.split(";");
		for(String oc : oldCookieArr){
			if(oc.isEmpty()){
				continue;
			}
			boolean notInNew = true;
			String oName = oc.split("=")[0];
			for(String nc : newCookieArr){
				if(nc.isEmpty()){
					continue;
				}
				String nName = nc.split("=")[0];
				if(nName.equals(oName)){
					notInNew = false;
					break;
				}
			}
			if(notInNew){
				newCookies = newCookies + oc + ";";
			}
		}
		return newCookies;
	}
}
