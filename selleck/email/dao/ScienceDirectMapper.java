package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.ScienceDirect;

public interface ScienceDirectMapper {
	List<ScienceDirect> selectBySearchPublication(Criteria criteria);
	
	List<ScienceDirect> selectBySearchRecord(Criteria criteria);
	
	void saveArticleAndAuthor(Article article,List<Author> authorList,ScienceDirect scienceDirect);
	
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,ScienceDirect scienceDirect);
	
	int insertArticle (Article article);
	
	int insertAuthor (Author author);
	
	int insertArticleAuthorRel (ArticleAuthorRel rel);
	
	void setRead(ScienceDirect scienceDirect);
	
	void setSearchRecordRead(ScienceDirect scienceDirect);
	
	void saveScienceDirect(ScienceDirect scienceDirect);
	
	void saveScienceDirectByKeyword(ScienceDirect scienceDirect);
	
	void updateScienceDirect(ScienceDirect scienceDirect);
}
