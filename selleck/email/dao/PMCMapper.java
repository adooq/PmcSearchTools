package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.PMC;

public interface PMCMapper {
	List<PMC> selectBySearchPublication(Criteria criteria);
	
	List<PMC> selectBySearchRecord(Criteria criteria);
	
	void saveArticleAndAuthor(Article article,List<Author> authorList,PMC pmc);
	
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,PMC pmc);
	
	int insertArticle (Article article);
	
	int insertAuthor (Author author);
	
	int insertArticleAuthorRel (ArticleAuthorRel rel);
	
	void setRead(PMC pmc);
	
	void setSearchRecordRead(PMC pmc);
	
	void savePMC(PMC pmc);
	
	void savePMCByKeyword(PMC pmc);
	
	void updatePMC(PMC pmc);
}
