package selleck.email.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.handle.model.Criteria;
import selleck.email.pojo.AuthorEmailSuffix;
import selleck.email.pojo.OrganizationSuffix;
import selleck.email.service.IGeneralService;
import selleck.email.service.impl.GeneralServiceImpl;

public class FindOrganizationSuffix {

	/** 找出非通讯作者（包含一些通讯作者）的里找不到email后缀的
	 * create table author_email_no_suffix as select * from author_email_suffix where EMAIL_SUFFIX = '' GROUP BY TITLE
	 * 找出来后，把地址取第一个逗号前的部分（一般是大学或医院名字），再去organization_suffix找含有这部分的地址的email后缀，
	 * 挑出现次数最多的作为这个地址的email后缀。
	 * @param args
	 */
	public static void main(String[] args) {
		IGeneralService  generalService= new GeneralServiceImpl();
		Criteria criteria = new Criteria();
		int startIndex = 1;
		int step = 1000;
		while(startIndex <= 520033){ // author_email_no_suffix max(id)
		// while(startIndex == 1){ // for test
			criteria.setWhereClause(" id >= " + startIndex + " and id < "+(startIndex + step));
			startIndex += step;
			List<AuthorEmailSuffix> aesList = generalService.selectAuthorEmailNoSuffix(criteria);
			for(AuthorEmailSuffix aes : aesList){
				if(aes.getAddress() != null && !aes.getAddress().trim().isEmpty()){
					String addressFirst = aes.getAddress().split(",")[0].trim();
					Criteria criteria1 = new Criteria();
					criteria1.setWhereClause(" INSTR(address,'"+addressFirst+"') > 0 and flag = 1");
					// System.out.println("addressFirst: "+addressFirst);
					List<OrganizationSuffix> osList = generalService.selectOrganizationSuffix(criteria1);
					if(osList.isEmpty()){
						continue;
					}
					int maxCount = 0;
					String maxCountSuffix = null;
					Map<String,Integer> osMap = new HashMap<String,Integer>(); // Map<邮箱后缀,OrganizationSuffix.count的和>
					for(OrganizationSuffix os : osList){
						if(osMap.containsKey(os.getSuffix())){
							osMap.put(os.getSuffix(), osMap.get(os.getSuffix()) + os.getCount());
						}else{
							osMap.put(os.getSuffix(), os.getCount());
						}
					}
					for(Map.Entry<String,Integer> entry : osMap.entrySet()){
						if(entry.getValue() > maxCount){
							maxCount = entry.getValue();
							maxCountSuffix = entry.getKey();
						}
					}
					aes.setEmailSuffix(maxCountSuffix);
					generalService.updateAuthorEmailNoSuffix(aes);
				}
				
				System.out.println("id: "+aes.getId());
			}
		}
		
	}

}
