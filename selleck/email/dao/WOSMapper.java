package selleck.email.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.NewEmail;
import selleck.email.pojo.WOS;

public interface WOSMapper {
	
	List<WOS> selectByExample(Criteria criteria);
	List<WOS> selectFromSearchRecord(Criteria criteria);
	String findAddressByAuthorName(Criteria criteria);
	void saveWOS(WOS wos);
	void saveWOSByKeyword(WOS wos);
	
	int updateArticle(@Param("article") Article article, @Param("condition") Map<String, Object> condition);
	
	int insertArticle (Article article);
	
	int insertAuthor (Author author);
	
	int insertArticleAuthorRel (ArticleAuthorRel rel);
	
	WOS selectByEmail(Criteria criteria);
	
	List<String> selectNewEmail(Criteria criteria);
	
	void updateNewEmail(NewEmail newEmail);
	
	void setRead(WOS wos);
	
	
	int insertArticleNotUseDB (Article article);
	
	int insertAuthorNotUseDB (Author author);
	
	int insertArticleAuthorRelNotUseDB (ArticleAuthorRel rel);
	
	void setSearchRecordRead(WOS wos);
	
}