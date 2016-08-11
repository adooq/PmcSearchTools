package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.ArticleMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.service.IArticleService;
import selleck.utils.Constants;

public class ArticleServiceImpl implements IArticleService{
	private String db;

	public ArticleServiceImpl(){
		this.db = Constants.LIFE_SCIENCE_DB;
	}
	
	public ArticleServiceImpl(String db){
		this.db = db;
	}
	
	@Override
	public List<Article> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<Article> retun = new ArrayList<Article>();
		try{
			ArticleMapper mapper = session.getMapper(ArticleMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void updateArticle(Article article) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			ArticleMapper mapper = session.getMapper(ArticleMapper.class);
			if(mapper!=null){
				mapper.updateArticle(article);
			}
		}finally{session.close();}
		
	}

	@Override
	public List<Article> findArticleByAuthor(Author author) {
		SqlSession session = MybatisFactory.getSession(db);
		List<Article> articles = null;
		try{
			ArticleMapper mapper = session.getMapper(ArticleMapper.class);
			if(mapper!=null){
				articles = mapper.findArticleByAuthor(author);
			}else{
				articles = null;
			}
		}finally{session.close();}
		
		return articles;
	}

	@Override
	public List<ArticleAuthorRel> findArticleRelByAuthor(Author author) {
		SqlSession session = MybatisFactory.getSession(db);
		List<ArticleAuthorRel> articles = null;
		try{
			ArticleMapper mapper = session.getMapper(ArticleMapper.class);
			if(mapper!=null){
				articles = mapper.findArticleRelByAuthor(author);
			}else{
				articles = null;
			}
		}finally{session.close();}
		
		return articles;
	}

	@Override
	public int selectMaxId() {
		SqlSession session = MybatisFactory.getSession(db);
		int maxId = 0;
		try{
			ArticleMapper mapper = session.getMapper(ArticleMapper.class);
			if(mapper!=null){
				maxId = mapper.selectMaxId();
			}
		}finally{session.close();}
		
		return maxId;
	}

}
