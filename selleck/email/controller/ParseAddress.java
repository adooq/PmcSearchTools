package selleck.email.controller;

import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;

import common.handle.model.Criteria;
import selleck.email.pojo.Author;
import selleck.email.service.IAuthorService;
import selleck.email.service.impl.AuthorServiceImpl;

/*
 * @author lyh
 * 2016.04
 * 从作者表中的地址解析出国家 、机构。多个地址只取第一个。
 * 
 * */
public class ParseAddress {

	public static void main(String[] args) throws Exception {

		IAuthorService authorService = new AuthorServiceImpl();
		List<Author> authors = null;
		int maxid = 83234236;
		int step = 50000;
		for (int i = 5206858; i < maxid; i += step) {
			Criteria criteria = new Criteria();
			criteria.setWhereClause("id > " + i + " and id <= " + (i + step));
			authors = authorService.selectByExample(criteria);
			for (Author author : authors) {
				try {
					parseAddress(author);
					authorService.saveAuthor(author);
				} catch (PersistenceException e) {
					e.printStackTrace();
					System.out.println(" 重复了");
					continue;
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}

			}
		}
	}

	/**
	 * 根据地址解析出国家 、机构。多个地址只取第一个。
	 * 
	 * @param author
	 */
	private static void parseAddress(Author author) {
		String address = author.getAddress();

		if (address == null || address.isEmpty()) {
			System.out.println("地址为空");
			return;
		}
		address = address.split("\\|")[0];

		parseOrganization(address, author);

		address = address.substring(0, address.indexOf("增强"));
		address = strictlyTrim(address);
		author.setAddress(address);

		parseCountry(address, author);
	}

	private static void parseCountry(String address, Author author) {
		try {
			// 找到增强、分号等的位置，把后面的都去掉。
			int position = findMinPosition(address);
			address = address.substring(0, position);

			String country = address.substring(address.lastIndexOf(',') + 1).trim();
			if (country.contains("USA") || country.contains("United States")) {
				country = "USA";
			} else if (country.contains("China")) {
				country = "China";
			} else if (country.contains(";")) {
				country = country.substring(0, country.indexOf(";"));
			}
			if (country.length() < 30) {
				author.setCountry(country);
			}
		} catch (Exception e) {
			System.out.println("解析出错：" + author.getAddress());
			e.printStackTrace();
		}
	}

	private static void parseOrganization(String address, Author author) {
		String organization = "";
		if (author.getOrganization() != null && !author.getOrganization().isEmpty()) {
			return;
		}
		if (address.indexOf("增强") > 0) {
			organization = address.substring(address.indexOf("增强组织信息的名称") + 9, address.length()).replaceAll(" ", "");
			organization = strictlyTrim(organization);
			author.setOrganization(organization);
		}
	}

	private static int findMinPosition(String address) {
		int position = address.length();
		int[] positions = { address.indexOf("增强"), address.indexOf(";"), address.indexOf(" Tel"),
				address.indexOf("E-mail"), address.indexOf("Email") };
		for (int j : positions) {
			if (j <= 0) {
				continue;
			} else {
				position = (j < position) ? j : position;
			}
		}
		return position;
	}

	/**
	 * 去除首位非字母字符，包括数字和符号。
	 * 
	 * @param str
	 * @return
	 */
	public static String strictlyTrim(String str) {
		int start = 0;
		int end = str.length();
		while (start < end && !Character.isLetter(str.charAt(start))) {
			start++;
		}
		while (start < end && !Character.isLetter(str.charAt(end - 1))) {
			end--;
		}
		str = str.substring(start, end);
		return str;

	}

}
