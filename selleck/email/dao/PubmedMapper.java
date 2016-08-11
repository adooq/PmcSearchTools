package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.Pubmed;

public interface PubmedMapper {
	List<Pubmed> selectBySearchPublication(Criteria criteria);
	
	void saveArticleAndAuthor(Article article,List<Author> authorList,Pubmed pubmed);
	
	int insertArticle (Article article);
	
	int insertAuthor (Author author);
	
	int insertArticleAuthorRel (ArticleAuthorRel rel);
	
	void setRead(Pubmed pubmed);
	
	void savePubmed(Pubmed pubmed);
	
	void updatePubmed(Pubmed pubmed);
	
	void savePubmedByKeyword(Pubmed pubmed);
	
	void updatePubmedByKeyword(Pubmed pubmed);
}
