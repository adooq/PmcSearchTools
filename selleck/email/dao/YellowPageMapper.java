package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.YellowPage;

public interface YellowPageMapper {
	List<YellowPage> selectByExample(Criteria criteria);
	
	void setRead(YellowPage yp);
	
	void saveYellowPage(YellowPage yp);
	
	void updateYellowPage(YellowPage yp);
	
	// void saveArticleAndAuthor(Article article,List<Author> authorList,PMC pmc);
	
	// int insertArticle (Article article);
		
	// int insertAuthor (Author author);
		
	// int insertArticleAuthorRel (ArticleAuthorRel rel);
		
}
