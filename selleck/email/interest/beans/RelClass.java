package selleck.email.interest.beans;

/**
 * 处理产品、小靶点、大靶点之间关系
 * @author fscai
 *
 */
public class RelClass {
	private Integer childId;
	private Integer parentId;
	private Integer score; // 从属关系的匹配度打分
	
	public RelClass(){
		
	}
	
	public RelClass(Integer cid, Integer pid){
		this.childId = cid;
		this.parentId = pid;
	}
	
	public RelClass(Integer cid, Integer pid,Integer score){
		this.childId = cid;
		this.parentId = pid;
		this.score = score;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + this.getChildId();
		result = PRIME * result + this.getParentId();
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RelClass) {
			if (((RelClass) obj).getChildId().equals(this.getChildId()) && ((RelClass) obj).getParentId().equals(this.getParentId())) {
				return true;
			}
		}
		return false;
	}
	public Integer getChildId() {
		return childId;
	}
	public void setChildId(Integer childId) {
		this.childId = childId;
	}
	public Integer getParentId() {
		return parentId;
	}
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}
	
	
}
