package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Author;

public interface AuthorMapper {
	
	List< Author> selectByExample(Criteria criteria);
	List< Author> selectNoDup(Criteria criteria);
	void updateAuthor(Author author);
	
	void insertAuthor(Author author);
	void changeEmails(int authorId);
	
	int selectMaxId();

	void saveAuthor(Author author);
	void deleteAuthor(Author author);
}