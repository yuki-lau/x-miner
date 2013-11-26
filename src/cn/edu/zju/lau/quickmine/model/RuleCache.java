package cn.edu.zju.lau.quickmine.model;

import java.util.Map;

import cn.edu.zju.lau.utils.LRUCache;

/**
 * 存放关联规则，prefix采用LRU替换策略，suffix采用LFU替换策略
 * @author yuki
 * @date 2013-11-26
 */
public class RuleCache {
	
	private LRUCache<String, SuffixList> rules;	// 存储Rules：Key为A|B形式的prefix，Value为预测的后缀列表
	private int maxCapacity;					// Cache的容量
	private int maxSuffixCapacity;				// 每个Prefix的SuffixList的容量
	
	public RuleCache(int maxCapacity, int maxSuffixCapacity){
		this.maxCapacity = maxCapacity;
		this.maxSuffixCapacity = maxSuffixCapacity;
		rules = new LRUCache<String, SuffixList>(this.maxCapacity);
	}
	
	@Override
	public String toString(){
		
		StringBuilder sb = new StringBuilder();
		
		for(Map.Entry<String, SuffixList> entry: rules.entrySet()){
			sb.append(entry.getKey()).append("->").append(entry.getValue()).append("\n");
		}
		
		return sb.toString();
	}
	
	public boolean addRule(String prefix, String suffix){
		
		boolean isNewRule = true;
		
		SuffixList suffixeList = rules.get(prefix);
		if(suffixeList == null){
			suffixeList = new SuffixList(maxSuffixCapacity);
		}
		else{
			isNewRule = false;
		}
		suffixeList.add(suffix);
		rules.put(prefix, suffixeList);
		
		return isNewRule;
	}

	
	/* getters and setters */
	
	public LRUCache<String, SuffixList> getRules() {
		return rules;
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public int getMaxSuffixCapacity() {
		return maxSuffixCapacity;
	}
}
