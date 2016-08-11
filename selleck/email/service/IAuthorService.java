package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Author;

public interface IAuthorService {
	List<Author> selectByExample(Criteria criteria);
	void updateAuthor(Author author);
	List<Author> selectNoDup(Criteria criteria);
	void insertAuthor(Author author);
	void changeEmails(int authorId);
	int selectMaxId();
	void saveAuthor(Author author);
	void deleteAuthor(Author author);
	
}
