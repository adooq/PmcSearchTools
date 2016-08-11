package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.Wiley;

public interface WileyMapper {
	List<Wiley> selectFromSearchPublication(Criteria criteria);
	
	List<Wiley> selectFromSearchRecord(Criteria criteria);
	
	void saveArticleAndAuthor(Article article,List<Author> authorList,Wiley wiley);
	
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,Wiley wiley);
	
	int insertArticle (Article article);
	
	int insertAuthor (Author author);
	
	int insertArticleAuthorRel (ArticleAuthorRel rel);
	
	void setRead(Wiley wiley);
	
	void setSearchRecordRead(Wiley wiley);
	
	void saveWiley(Wiley wiley);
	
	void saveWileyByKeyword(Wiley wiley);
	
	void updateWiley(Wiley wiley);
}
