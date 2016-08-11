package selleck.email.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTextArea;

import common.handle.frame.CreateFrame;
import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.AuthorInterest;
import selleck.email.service.IArticleService;
import selleck.email.service.IAuthorInterestService;
import selleck.email.service.IAuthorService;
import selleck.email.service.impl.ArticleServiceImpl;
import selleck.email.service.impl.AuthorInterestServiceImpl;
import selleck.email.service.impl.AuthorServiceImpl;


/**
 * 通过selleck_edm_article,selleck_edm_author,selleck_edm_article_author_rel来确定一个作者兴趣点，记录在selleck_edm_interest
 * 取两篇文章的三个兴趣点作为这个作者的六个兴趣点。
      取哪两篇文章的规则：优先取这个作者是通讯作者的文章，然后判断这个作者是第一作者的文章再加120分，第二作者再加100分，取总分高的。
 * @author fscai
 *
 */
public class FindInterestsForAuthor {
	public static final JTextArea textArea = CreateFrame.getFrame().getLoggerTA();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IAuthorService authorService = new AuthorServiceImpl();
		IArticleService articleService = new ArticleServiceImpl();
		Criteria criteria = new Criteria();
		int startIndex = 12000001;
		int step = 1000;
		// while(startIndex <= 13081138){ // MAX(id) from selleck_edm_author_nodup
		while(startIndex < 13081138){
			criteria.setWhereClause(" id >= " + startIndex + " and id < "+(startIndex + step));
			startIndex += step;
			List<Author> authors = authorService.selectNoDup(criteria);
			for(Author author : authors){
				Criteria criteria1 = new Criteria();
				String fullName = author.getFullName().replace("'", "\\'").replace("\"", "\\\"");
				String email = author.getEmail().replace("'", "\\'").replace("\"", "\\\"");
				criteria1.setWhereClause(" full_name = '"+fullName+"' and email = '"+email+"' ");
				List<Author> sameNameEmailAuthors = authorService.selectByExample(criteria1); // 相同全名和eamail就认为是同一个作者		
				List<Article> articles = new ArrayList<Article>(); // 同一作者写的文章
				List<ArticleAuthorRel> rels = new ArrayList<ArticleAuthorRel>(); // 作者与文章的关系
				for(Author sameNameEmailAuthor : sameNameEmailAuthors){
					articles.addAll(articleService.findArticleByAuthor(sameNameEmailAuthor));
					rels.addAll(articleService.findArticleRelByAuthor(sameNameEmailAuthor));
				}

				// System.out.println("rels: "+rels);
				
				Map<Integer,Article> articleMap = new HashMap<Integer,Article>();
				for(Article article : articles){
					articleMap.put(article.getId(), article);
				}
				
				
				// 取两篇文章的三个兴趣点作为这个作者的六个兴趣点。
			    // 取哪两篇文章的规则：优先取这个作者是通讯作者的文章，然后判断这个作者是第一作者的文章加120分，第二作者加100分，取总分高的。
				List<Article> finalTwoArticles = new ArrayList<Article>(); // 最终选出的两篇文章
				List<Article> cArticle = new ArrayList<Article>(); // 是通讯作者的文章
				List<Article> ncArticle = new ArrayList<Article>(); // 不是通讯作者的文章
				
				// 分出是和不是通讯作者的文章，并按分数排序
				for(ArticleAuthorRel rel : rels){
					Article a = articleMap.get(rel.getArticleId());
					a.setAuthorId(author.getId());
					if(rel.getPriority() == 0){
						cArticle.add(a);
					}else if(rel.getPriority() == 1){
						a.setScore(a.getScore() + 120); // 是第一作者的文章加120分
						ncArticle.add(a);
					}else if(rel.getPriority() == 2){
						a.setScore(a.getScore() + 100); // 第二作者加100分
						ncArticle.add(a);
					}else if(rel.getPriority() == 3){
						ncArticle.add(a);
					}
				}
				Collections.sort(cArticle); // 从小到大排序
				Collections.reverse(cArticle); // 从大到小
				Collections.sort(ncArticle); // 从小到大排序
				Collections.reverse(ncArticle); // 从大到小
				
				// System.out.println("cArticle: "+cArticle);
				// System.out.println("ncArticle: "+ncArticle);
				
				// 如果是通讯作者的文章数量大于等于2，数量足够，就选分数前2的。如果<=2，要去找不是通讯作者的文章，补足到finalTwoArticles
				if(cArticle.size() == 0){
					if(ncArticle.size() >= 2){
						finalTwoArticles.add(ncArticle.get(0));
						finalTwoArticles.add(ncArticle.get(1));
					}else if(ncArticle.size() >= 1){
						finalTwoArticles.add(ncArticle.get(0));
					}
				}else if(cArticle.size() == 1){
					if(ncArticle.size() >= 1){
						finalTwoArticles.add(ncArticle.get(0));
					}
					finalTwoArticles.add(cArticle.get(0));
				}else{
					finalTwoArticles.add(cArticle.get(0));
					finalTwoArticles.add(cArticle.get(1));
				}
				
				// System.out.println("finalTwoArticles: "+finalTwoArticles);
				
				if(!finalTwoArticles.isEmpty()){
					IAuthorInterestService aiService = new AuthorInterestServiceImpl();
					AuthorInterest a1 = null;
					AuthorInterest a2 = null;
					a1 = new AuthorInterest();
					Article article1 = finalTwoArticles.get(0);
					a1.setAuthorId(article1.getAuthorId());
					a1.setBig(article1.getBig());
					a1.setProduct(article1.getProduct());
					a1.setScore(article1.getScore());
					a1.setSmall(article1.getSmall());
					
					if(finalTwoArticles.size() == 2){
						a2 = new AuthorInterest();
						Article article2 = finalTwoArticles.get(1);
						a2.setAuthorId(article2.getAuthorId());
						a2.setBig(article2.getBig());
						a2.setProduct(article2.getProduct());
						a2.setScore(article2.getScore());
						a2.setSmall(article2.getSmall());
					}
					aiService.saveAuthorInterest(a1, a2);
				}
				
				textArea.append("author id: "+ author.getId()+"\n");
				// System.out.println("author id: "+ author.getId());
				// System.out.println();
			}
		}
	}

}
