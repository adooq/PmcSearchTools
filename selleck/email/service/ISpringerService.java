package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.Springer;

public interface ISpringerService {
	List<Springer> selectBySearchPublication(Criteria criteria);
	List<Springer> selectBySearchRecord(Criteria criteria);
	void saveArticleAndAuthor(Article article,List<Author> authorList,Springer springer);
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,Springer springer);
	void saveSpringer(Springer springer);
	void saveSpringerByKeyword(Springer springer, BrandTitleTemp brandTitleTemp);
	void updateSpringer(Springer springer);
}
