package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.NewEmail;
import selleck.email.pojo.WOS;

public interface IWOSService {
	List<WOS> selectByExample(Criteria criteria);
	void saveArticleAndAuthor(Article article,List<Author> authorList,WOS wos);
	WOS selectWOSByNewEmail(Criteria criteria);
	void saveWOS(WOS wos);
	void saveWOSByKeyword(WOS wos, BrandTitleTemp brandTitleTemp);
	
	// 从search_record查询出wos文章
	List<WOS> selectFromSearchRecord(Criteria criteria);
	void saveArticleAndAuthorFromSearchRecord(Article article, List<Author> authorList,WOS wos);
	
	// 从252 emailselleck库查author_email_suffix
	String findAddressByAuthorName(Criteria criteria);

	// t_wos_new_email
	List<String> selectNewEmail(Criteria criteria);
	void updateNewEmail(NewEmail newEmail);
}
