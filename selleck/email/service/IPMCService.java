package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.PMC;

public interface IPMCService {
	List<PMC> selectBySearchPublication(Criteria criteria);
	List<PMC> selectBySearchRecord(Criteria criteria);
	void saveArticleAndAuthor(Article article,List<Author> authorList,PMC pmc);
	void saveArticleAndAuthorForSearchRecord(Article article,List<Author> authorList,PMC pmc);
	void savePMC(PMC pmc);
	void savePMCByKeyword(PMC pmc, BrandTitleTemp brandTitleTemp);
	void updatePMC(PMC pmc);
}
