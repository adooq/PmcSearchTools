package selleck.email.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.JTextArea;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import common.handle.model.Criteria;
import selleck.email.pojo.CNKI;
import selleck.email.service.ICNKIService;
import selleck.email.service.impl.CNKIServiceImpl;

public class DownloadCNKIpdfController {

	private boolean startFlag = false;// 本线程开始标志

	/**
	 * 
	 * @param startId  文章id,从startId的文章开始下载pdf
	 * @param cookie
	 */
	public void downloadCNKIpdf(String startId, String cookie,JTextArea logger) {
		int startIndex = 0;
		int step = 1000;
		int count = 0;
		if(startId != null && !startId.isEmpty()){
			startIndex = Integer.valueOf(startId) - 1;
		}
		ICNKIService cnkiService = new CNKIServiceImpl();
		while(true){ 
			Criteria criteria = new Criteria();
			criteria.setOracleStart(startIndex + step*count);
			criteria.setOracleEnd(step);
			
			List<CNKI> cnkiList = cnkiService.selectByExample(criteria);
			if(cnkiList == null || cnkiList.size() == 0){
				break;
			}
			
			for (CNKI cnki : cnkiList) {
				System.out.print(cnki.getId() + " " + cnki.getTitle());
				logger.append(cnki.getId() + " " + cnki.getTitle());
				downloadPDF(cnki.getPdf_url(), cookie, String.valueOf(cnki.getId()), logger);
			}
			count ++;
		}
	}
	
	/**
	 * 
	 * @param publication
	 * @param cookie
	 */
	public void downloadCNKIpdfForWangguo(String publication, String cookie,JTextArea logger) {
		int startIndex = 0;
		int step = 1000;
		
		while(true){
			Criteria criteria = new Criteria();
			if(publication != null && !publication.isEmpty()){
				criteria.put("publication", publication);
			}
			criteria.setOracleStart(startIndex);
			criteria.setOracleEnd(step);
			ICNKIService cnkiService = new CNKIServiceImpl();
			List<CNKI> cnkiList = cnkiService.selectForWangguo(criteria);
			if(cnkiList == null || cnkiList.size() == 0){
				break;
			}
			
			for (CNKI cnki : cnkiList) {
				System.out.print("title: " + cnki.getTitle());
				logger.append("title: " + cnki.getTitle());
				downloadPDF(cnki.getPdf_url(), cookie, String.valueOf(cnki.getId()), logger);
			}
			
			startIndex += step;
		}
	}

	/**
	 * 下载pdf
	 * 
	 * @param pdfURL
	 *            CNKI对象中的pdf_url
	 * @param cookie
	 *            下载需要的cookie
	 * @param fileName
	 *            文件名，用CNKI的id作为文件名
	 */
	private void downloadPDF(String pdfURL, String cookie, String fileName, JTextArea logger) {
			CloseableHttpClient httpclient = null;
			File storeFile = new File(DownloadCNKIpdfMain.PDF_DIR +  fileName + ".pdf");
			if (!storeFile.exists()) {
				httpclient = HttpClientBuilder.create().build();
				HttpGet httpGet = new HttpGet(pdfURL);
				httpGet.setHeader("Cookie", cookie);
				HttpResponse response = null;
				RequestConfig requestConfig = RequestConfig.custom()  
					    .setConnectionRequestTimeout(300000).setConnectTimeout(300000)  
					    .setSocketTimeout(300000).build();  
				httpGet.setConfig(requestConfig);  
				try {
					response = httpclient.execute(httpGet);
				} catch (ClientProtocolException e1) {
					e1.printStackTrace();
					System.out.println("httpclient.execute(httpGet) ClientProtocolException");
					logger.append(" httpclient.execute(httpGet) ClientProtocolException\n");
					if(httpclient != null){
						try {
							httpclient.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(DownloadCNKIpdfMain.timeInterval);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return;
				} catch (IOException e1) {
					System.out.println("httpclient.execute(httpGet) IOException");
					logger.append(" httpclient.execute(httpGet) IOException\n");
					e1.printStackTrace();
					if(httpclient != null){
						try {
							httpclient.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(DownloadCNKIpdfMain.timeInterval);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return;
				}

				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
					// 请求成功
					// 取得请求内容
					HttpEntity entity = response.getEntity();

					// 显示内容
					System.out.print("  " + entity.getContentType());
					logger.append("  " + entity.getContentType());
					if (entity != null && entity.getContentType().toString().endsWith("pdf") && entity.isStreaming()) {
						// 这里可以得到文件的类型 如image/jpg /zip /tiff 等等
						// 但是发现并不是十分有效，有时明明后缀是.rar但是取到的是null，这点特别说明
						// System.out.println(entity.getContentType());
						// 可以判断是否是文件数据流
						// System.out.println(entity.isStreaming());
						// 设置本地保存的文件
						
						FileOutputStream output = null;
						try {
							storeFile.createNewFile();
							output = new FileOutputStream(storeFile);
							// 得到网络资源并写入文件
							InputStream input = entity.getContent();
							byte b[] = new byte[DownloadCNKIpdfMain.BUFFER];
							int j = 0;
							while ((j = input.read(b)) != -1) {
								output.write(b, 0, j);
							}
						} catch (IOException e) {
							System.out.println("FileOutputStream IOException");
							logger.append(" FileOutputStream IOException\n");
							e.printStackTrace();
							try {
								Thread.sleep(DownloadCNKIpdfMain.timeInterval);
							} catch (InterruptedException e1) {
								e.printStackTrace();
							}
							return;
						}finally{
							try {
								output.flush();
								output.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						
						

						System.out.println("  下载成功");
						logger.append("  下载成功" + "\n");

						

					}else if(entity != null && entity.getContentType().toString().contains("html")){
						// 有些pdf涉及保密内容，不能下载 ，也生成一个临时文件做标记
						// vm4 25000 碳水化合物和饱和脂肪酸摄入与心肌梗死风险:血糖指数的重要性
						// vm2 60000 急诊经皮冠状动脉介入治疗前强化阿托伐他汀对急性心肌梗死患者的中期疗效观察
						InputStream input;
						try {
							input = entity.getContent();
							byte b[] = new byte[DownloadCNKIpdfMain.BUFFER];
							if (( input.read(b)) != -1) {
								String htmlStr = new String(b,"utf-8");
								if(htmlStr.contains("文件涉及保密内容")){
									storeFile.createNewFile();
									System.out.println("  文件涉及保密内容");
									logger.append("  文件涉及保密内容" + "\n");
								}else{
									System.out.println("  下载文件不是pdf");
									logger.append("  下载文件不是pdf" + "\n");
								}
							}
						} catch (IllegalStateException e) {
							System.out.println("IllegalStateException");
							logger.append(" IllegalStateException\n");
							e.printStackTrace();
						} catch (IOException e) {
							System.out.println("html read IOException");
							logger.append(" html read IOException\n");
							e.printStackTrace();
						}
						
					}else{
						System.out.println("  下载文件不是pdf");
						logger.append("  下载文件不是pdf" + "\n");
					}
					
					try {
						EntityUtils.consume(entity);
					} catch (IOException e) {
						System.out.println("  EntityUtils.consume(entity)  IOException");
						logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
						e.printStackTrace();
					}
				}else{
					System.out.println("  下载请求失败");
					logger.append("  下载请求失败" + "\n");
				}
				try {
					Thread.sleep(DownloadCNKIpdfMain.timeInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("  文件已存在");
				logger.append("  文件已存在" + "\n");
			}
			
			
//		} catch (ClientProtocolException e) {
//			System.out.println("ClientProtocolException");
//			logger.append(" ClientProtocolException");
//			e.printStackTrace();
//		} catch (IOException e) {
//			System.out.println("IOException");
//			logger.append(" IOException");
//			e.printStackTrace();
//		}finally{
//			if(httpclient != null){
//				try {
//					httpclient.close();
//				} catch (IOException e) {
//					System.out.println("CloseableHttpClient IOException");
//					logger.append("CloseableHttpClient IOException");
//					e.printStackTrace();
//				}
//			}
//		}
	}

	public boolean isStartFlag() {
		return startFlag;
	}

	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}

}
