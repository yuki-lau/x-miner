package cn.edu.zju.lau.cminer.model;

import java.util.HashSet;
import java.util.Set;

/**
 * 记录每个候选频繁序列（HDFS文件访问序列）在window中的最大后缀
 * 
 * @author yuki
 * @date 2013-11-22
 */
public class HDFSSubseqSuffix {

	private String subsequence;
	private int occurTimes;
	private Set<String> suffixes;
	
	public HDFSSubseqSuffix(){
		occurTimes = 0;
		suffixes = new HashSet<String>();
	}
	
	public HDFSSubseqSuffix(String subsequence, int occurTimes, Set<String> suffixes){
		this.subsequence = subsequence;
		this.occurTimes = occurTimes;
		this.suffixes = suffixes;
	}

	public void addSuffix(String suffix) {
		this.suffixes.add(suffix);
	}
	
	public String toString(){
		return suffixes.toString();
	}
	public String getSubsequence() {
		return subsequence;
	}

	public void setSubsequence(String subsequence) {
		this.subsequence = subsequence;
	}

	public int getOccurTimes() {
		return occurTimes;
	}

	public void setOccurTimes(int occurTimes) {
		this.occurTimes = occurTimes;
	}

	public Set<String> getSuffixes() {
		return suffixes;
	}

	public void setSuffixes(Set<String> suffixes) {
		this.suffixes = suffixes;
	}
}
