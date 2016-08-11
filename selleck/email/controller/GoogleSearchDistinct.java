package selleck.email.controller;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.GoogleSearch;
import selleck.email.service.IArticleService;
import selleck.email.service.IAuthorService;
import selleck.email.service.IGoogleSearchService;
import selleck.email.service.impl.ArticleServiceImpl;
import selleck.email.service.impl.AuthorServiceImpl;
import selleck.email.service.impl.GoogleSearchServiceImpl;
import selleck.utils.StringUtils;

public class GoogleSearchDistinct {

	
	public static void main(String[] args) {
		ignoreCcorrespondingAuthor();
	}
	
	/**
	 * 查询252.emailhunter的selleck_edm_article表，根据CORRESPONDINGAUTHOR和TITLE_INDEX，
	 * 去除252.emailselleck的google_search表中的通讯作者
	 */
	private static void ignoreCcorrespondingAuthor(){
		IGoogleSearchService gsService = new GoogleSearchServiceImpl();
		IArticleService articleService = new ArticleServiceImpl();
		IAuthorService authorService = new AuthorServiceImpl();
		
		Criteria criteria = new Criteria();
		int idIndex = 1;
		int step = 100000;
		while(idIndex <= 1){ // SELECT MAX(id) from google_search
		// while(idIndex == 16221690){ // for test
			criteria.setWhereClause(" id >= "+idIndex+"  and id < "+ (idIndex+step));
			idIndex = idIndex+step;
			List<GoogleSearch> gsList = gsService.selectByCriteria(criteria);
			System.out.println("idIndex: "+idIndex);
			for(GoogleSearch gs : gsList){
				String fullName = gs.getFullAuthor();
				fullName = fullName.replaceAll(",", ", "); // 为,后面加空格，为了美观
				fullName = fullName.replaceAll("\\s+", " ");
				fullName = StringUtils.toSqlForm(fullName);
				String titleIndexString = gs.getTitle().length() >= 250 ? gs.getTitle().substring(0, 250) : gs.getTitle();
				titleIndexString = StringUtils.toSqlForm(titleIndexString);
				
				Criteria authorCriteria = new Criteria();
				authorCriteria.setWhereClause(" full_name = '"+fullName+"' limit 1");
				// System.out.println("fullName "+fullName);
				List<Author> authors = authorService.selectByExample(authorCriteria);
				// System.out.println("authors size: "+authors.size());
				if(authors.isEmpty()){
					continue;
				}else{
					String cName = authors.get(0).getShortName();
					cName = StringUtils.toSqlForm(cName);
					Criteria articleCriteria = new Criteria();
					
					articleCriteria.setWhereClause(" TITLE_INDEX = '"+titleIndexString+"' and CORRESPONDINGAUTHOR = '"+cName+"' limit 1 ");
					List<Article> articles = articleService.selectByExample(articleCriteria);
					// System.out.println("articles size: "+articles.size());
					if(!articles.isEmpty()){
						gs.setRealId(4);
						gsService.updateGoogleSearch(gs);
					}
				}
				
				
			}
		}
	}
	
	/**google_search里有id , flag ，name,html_title(email后缀) ，有些记录name和html_title会重复，flag3表示已经到google上搜过，flag1表示未搜过。
	 * name,html_title就认为是同一个作者，不需要重复抓。
	 * 把flag=1的，name和email都相同的记录的flag设置成3，但保留一条flag是1。
	 * 或者说就是把name和email都相同的记录去除（flag设置成3）,但要保留一条。 
	 */
	private static void distinctGoogleSearch(){
		IGoogleSearchService gsService = new GoogleSearchServiceImpl();
		Criteria criteria = new Criteria();
		
		List<GoogleSearch> dupList = gsService.selectDup(criteria);
		System.out.println("dupList size: "+dupList.size());
		
		Criteria criteria1 = new Criteria();
		int idIndex = 1; // local select MIN(id) from google_search where real_id = 1 
		int step = 10000;
		// while(idIndex <= 9567319){ // for local test
		while(idIndex <= 16221690){ // select MAX(id) from google_search
			criteria1.setWhereClause(" real_id = 1 and id >= "+idIndex+"  and id < "+ (idIndex+step));
			idIndex = idIndex+step;
			List<GoogleSearch> gsList =  gsService.selectByCriteria(criteria1);
			System.out.println("gsList size: "+gsList.size());
			for(GoogleSearch gs : gsList){
				int gsIndex = dupList.indexOf(gs);
				if(gsIndex != -1){ // 
					// System.out.println("dup gs: "+gs);
					GoogleSearch dupGs =  dupList.get(gsIndex);
					if(gs.getId() !=  dupGs.getId()){
						// System.out.println("  set real_id 3 ");
						gs.setRealId(3);
						gsService.updateGoogleSearch(gs);
					}
				}else{
					// System.out.println("no dup gs: "+gs);
				}
			}
		}
	}

}
