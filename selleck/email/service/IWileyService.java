package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.Wiley;

public interface IWileyService {
	List<Wiley> selectFromSearchPublication(Criteria criteria);
	List<Wiley> selectFromSearchRecord(Criteria criteria);
	void saveArticleAndAuthor(Article article,List<Author> authorList,Wiley wiley);
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,Wiley wiley);
	void saveWiley(Wiley wiley);
	void saveWileyByKeyword(Wiley wiley, BrandTitleTemp brandTitleTemp);
	void updateWiley(Wiley wiley);
}
