package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.ScienceDirect;

public interface IScienceDirectService {
	List<ScienceDirect> selectBySearchPublicaton(Criteria criteria);
	List<ScienceDirect> selectBySearchRecord(Criteria criteria);
	void saveArticleAndAuthor(Article article,List<Author> authorList,ScienceDirect scienceDirect);
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,ScienceDirect scienceDirect);
	void saveScienceDirect(ScienceDirect scienceDirect);
	void saveScienceDirectByKeyword(ScienceDirect scienceDirect, BrandTitleTemp brandTitleTemp);
	void updateScienceDirect(ScienceDirect scienceDirect);
}
