package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.Pubmed;

public interface IPubmedService {
	List<Pubmed> selectBySearchPublication(Criteria criteria);
	void saveArticleAndAuthor(Article article,List<Author> authorList,Pubmed pubmed);
	void savePubmed(Pubmed pubmed);
	void updatePubmed(Pubmed pubmed);
	void savePubmedByKeyword(Pubmed pubmed);
	void updatePubmedByKeyword(Pubmed pubmed);
}
