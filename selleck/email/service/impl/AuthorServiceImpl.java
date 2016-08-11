package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.AuthorMapper;
import selleck.email.pojo.Author;
import selleck.email.service.IAuthorService;
import selleck.utils.Constants;

public class AuthorServiceImpl implements IAuthorService {
	private String db;

	public AuthorServiceImpl() {
		this.db = Constants.LIFE_SCIENCE_DB;
	}

	public AuthorServiceImpl(String db) {
		this.db = db;
	}

	@Override
	public List<Author> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<Author> retun = new ArrayList<Author>();
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				retun = mapper.selectByExample(criteria);
			} else {
				retun = null;
			}
		} finally {
			session.close();
		}

		return retun;
	}

	@Override
	public void updateAuthor(Author author) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				mapper.updateAuthor(author);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public List<Author> selectNoDup(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<Author> retun = new ArrayList<Author>();
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				retun = mapper.selectNoDup(criteria);
			} else {
				retun = null;
			}
		} finally {
			session.close();
		}

		return retun;
	}

	@Override
	public void insertAuthor(Author author) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				mapper.insertAuthor(author);
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void saveAuthor(Author author) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				mapper.saveAuthor(author);
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void changeEmails(int authorId) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				mapper.changeEmails(authorId);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public int selectMaxId() {
		SqlSession session = MybatisFactory.getSession(db);
		int maxId = 0;
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				maxId = mapper.selectMaxId();
			}
		} finally {
			session.close();
		}

		return maxId;
	}

	@Override
	public void deleteAuthor(Author author) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			AuthorMapper mapper = session.getMapper(AuthorMapper.class);
			if (mapper != null) {
				mapper.deleteAuthor(author);
			}
		} finally {
			session.close();
		}
		
	}


}
