package selleck.email.controller;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleSearch;
import selleck.email.service.IGoogleSearchService;
import selleck.email.service.impl.GoogleSearchServiceImpl;

public class GoogleSearchFixFullAuthor {

	/**google_search 、 google_search_email里有些记录的作者全名full_author不正确，例如：Bode, AnnM.
	 * 应该是Bode, Ann M.
	 * 修改full_author的程序
	 * @param args
	 */
	public static void main(String[] args) {
		fixGoogleSearchFullAuthor();
		// fixGoogleSearchEmailFullAuthor();
	}
	
	private static void fixGoogleSearchFullAuthor(){
		IGoogleSearchService gsService = new GoogleSearchServiceImpl();
		int idIndex = 1;
		int step = 10000;
		Criteria criteria = new Criteria();
		Pattern p = Pattern.compile("\\B[A-Z]\\.",Pattern.CASE_INSENSITIVE);
		while(idIndex <= 16221690){ // select MAX(id) from google_search
		// while(idIndex == 16221690){ //for test
			criteria.setWhereClause(" id >= "+idIndex+"  and id < "+ (idIndex+step));
			idIndex = idIndex+step;
			List<GoogleSearch> gsList = gsService.selectByCriteria(criteria);
			System.out.println("gsList size: "+gsList.size());
			for(GoogleSearch gs : gsList){
				boolean found = false;
				String name = gs.getFullAuthor();
				Matcher matcher = p.matcher(gs.getFullAuthor());
				while(matcher.find()){
					found = true;
					name =  name.replace(matcher.group()," "+matcher.group());
					name = name.replaceAll("\\s+", " ");
				}
				if(found){
					gs.setFullAuthor(name);
					gsService.updateGoogleSearch(gs);
				}
			}
		}
	}
	
	private static void fixGoogleSearchEmailFullAuthor(){
		IGoogleSearchService gsService = new GoogleSearchServiceImpl();
		int idIndex = 1;
		int step = 10000;
		Criteria criteria = new Criteria();
		Pattern p = Pattern.compile("\\B[A-Z]\\.",Pattern.CASE_INSENSITIVE);
		while(idIndex <= 990960){ //select MAX(id) from google_search_email
		// while(idIndex == 1){ // for test
			criteria.setWhereClause(" id >= "+idIndex+"  and id < "+ (idIndex+step));
			idIndex = idIndex+step;
			List<GoogleSearch> gsList = gsService.selectGSEmailByCriteria(criteria);
			System.out.println("gsList size: "+gsList.size());
			for(GoogleSearch gs : gsList){
				boolean found = false;
				String name = gs.getFullAuthor();
				Matcher matcher = p.matcher(gs.getFullAuthor());
				while(matcher.find()){
					found = true;
					name =  name.replace(matcher.group()," "+matcher.group());
					name = name.replaceAll("\\s+", " ");
				}
				if(found){
					gs.setFullAuthor(name);
					gsService.updateGoogleSearchEmail(gs);
				}
			}
		}
	}

}
