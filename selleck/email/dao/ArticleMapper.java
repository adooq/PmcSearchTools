package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;

public interface ArticleMapper {
	
	List<Article> selectByExample(Criteria criteria);
	
	void updateArticle(Article article);
	
	List<Article> findArticleByAuthor(Author author);
	
	List<ArticleAuthorRel> findArticleRelByAuthor(Author author);
	
	int selectMaxId();
}