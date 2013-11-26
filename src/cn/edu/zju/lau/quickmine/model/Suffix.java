package cn.edu.zju.lau.quickmine.model;

/**
 * 关联规则：Ai & Aj -> Ak 中的Ak，即关联规则中的suffix
 * @author yuki
 * @date 2013-11-26
 */
public class Suffix {

	private String suffix;		// suffix代表的文件名，TODO: 替换为文件对象，挖掘更多信息
	private int support = 1;	// suffix发生的次数
	
	public Suffix(String suffix, int support){
		this.suffix = suffix;
		this.support = support;
	}
	
	@Override
	public String toString(){
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(suffix).append("(").append(support).append(")");
		
		return sb.toString();
	}

	
	/* getters and setters */
	
	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public int getSupport() {
		return support;
	}

	public void setSupport(int support) {
		this.support = support;
	}
	
}
