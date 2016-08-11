package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.Springer;

public interface SpringerMapper {
	List<Springer> selectBySearchPublication(Criteria criteria);
	
	List<Springer> selectBySearchRecord(Criteria criteria);
	
	void saveArticleAndAuthor(Article article,List<Author> authorList,Springer pmc);
	
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,Springer pmc);
	
	int insertArticle (Article article);
	
	int insertAuthor (Author author);
	
	int insertArticleAuthorRel (ArticleAuthorRel rel);
	
	void setRead(Springer springer);
	
	void setSearchRecordRead(Springer springer);
	
	void saveSpringer(Springer springer);
	
	void saveSpringerByKeyword(Springer springer);
	
	void updateSpringer(Springer springer);
}
