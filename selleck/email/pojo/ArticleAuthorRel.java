package selleck.email.pojo;

public class ArticleAuthorRel {
	private int articleId;
	private int authorId;
	private Integer priority = 3; // 作者在这篇文章中的重要性。 0通讯作者，1第一作者(作者中名字排第一个)，2第二作者(作者中名字排第二个)，3什么都不是
	
	public int getArticleId() {
		return articleId;
	}
	public void setArticleId(int articleId) {
		this.articleId = articleId;
	}
	public int getAuthorId() {
		return authorId;
	}
	public void setAuthorId(int authorId) {
		this.authorId = authorId;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "(article"+articleId+",author"+authorId+","+priority+")";
	}
	
	
}
