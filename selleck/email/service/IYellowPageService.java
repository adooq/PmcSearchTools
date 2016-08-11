package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.YellowPage;

public interface IYellowPageService {
	List<YellowPage> selectByExample(Criteria criteria);
	// void saveArticleAndAuthor(Article article,List<Author> authorList,YellowPage yellowPage);
	void saveYellowPage(YellowPage yp);
	void updateYellowPage(YellowPage yp);
}
