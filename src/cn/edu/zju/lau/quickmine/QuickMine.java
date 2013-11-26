package cn.edu.zju.lau.quickmine;

import java.util.ArrayList;
import java.util.List;

import cn.edu.zju.lau.quickmine.model.RuleCache;

/**
 * 实现论文《Context-Aware Prefetching at the Storage Server》中阐述的挖掘Block访问关联性的方法。
 * @author yuki
 * @date 2013-11-26
 */
public class QuickMine {
	
	private int maxGap;					// 关联序列间的最大间隔 < maxGap
	private int maxPrefixNum;			// Rule Cache中对多的prefix数量
	private int maxSuffixNum;			// Rule Cache中每个prefix对应的最多的suffix数量
	private int prefetchNum;			// 每次prefetch时，fetch的最多suffix数量
	private List<String> accessLogs;  	// 待挖掘的访问序列
	private RuleCache ruleCache;		// 存放生成的关联规则
	
	/**
	 * maxPrefixNum和maxSuffixNum必须在构造时指定，否则使用默认值，没有setters，因为需要用来创建RuleCache。
	 */
	public QuickMine(){
		this.maxGap = 5;
		this.maxPrefixNum = 1024;
		this.maxSuffixNum = 16;
		this.prefetchNum = 4;
		this.accessLogs = new ArrayList<String>();
		this.ruleCache = new RuleCache(this.maxPrefixNum, this.maxSuffixNum);
	}
	
	public QuickMine(int maxPrefixNum, int maxSuffixNum){
		this.maxPrefixNum = maxPrefixNum;
		this.maxSuffixNum = maxSuffixNum;
		this.maxGap = 5;
		this.prefetchNum = 4;
		this.accessLogs = new ArrayList<String>();
		this.ruleCache = new RuleCache(this.maxPrefixNum, this.maxSuffixNum);
	}
	
	public QuickMine(int maxPrefixNum, int maxSuffixNum, int maxGap, int prefetchNum){
		this.maxPrefixNum = maxPrefixNum;
		this.maxSuffixNum = maxSuffixNum;
		this.maxGap = maxGap;
		this.prefetchNum = prefetchNum;
		this.accessLogs = new ArrayList<String>();
		this.ruleCache = new RuleCache(this.maxPrefixNum, this.maxSuffixNum);
	}

	public void startMining(){
		
		// 检查输入日志序列
		if(accessLogs == null || accessLogs.size() == 0){
			System.err.println("Access Logs is null! Exit...");
			return;
		}
		
		// 根据规则 Ai & Aj -> Ak, i < j < k, j - i < maxGap, k - j < maxGap 生成规则
		for(int i = 0; i < accessLogs.size() - 2; i++){
			
			for(int j = i + 1; j < maxGap + i && j < accessLogs.size() - 1; j++){
				
				String prefix = accessLogs.get(i) + "|" + accessLogs.get(j);
				
				for(int k = j + 1; k < maxGap + j && k < accessLogs.size(); k++){
					
					String suffix = accessLogs.get(k);
					ruleCache.addRule(prefix, suffix);
				}
			}
		}
	}
	
	public static void main(String[] args){
		
		QuickMine miner = new QuickMine();
		List<String> logs = new ArrayList<String>();
		logs.add("a");
		logs.add("b");
		logs.add("c");
		logs.add("d");
		logs.add("e");
		logs.add("f");
		logs.add("g");
		logs.add("h");
		logs.add("i");
		logs.add("j");
		logs.add("a");
		logs.add("b");
		logs.add("c");
		logs.add("d");
		logs.add("e");
		logs.add("f");
		logs.add("g");
		logs.add("h");
		logs.add("i");
		logs.add("j");
		miner.setAccessLogs(logs);
		miner.startMining();
		
		System.out.println(logs);
		System.out.println(miner.getRuleCache());
	}
	
	/* getters and setters */
	
	public int getMaxGap() {
		return maxGap;
	}

	public void setMaxGap(int maxGap) {
		this.maxGap = maxGap;
	}

	public int getPrefetchNum() {
		return prefetchNum;
	}

	public void setPrefetchNum(int prefetchNum) {
		this.prefetchNum = prefetchNum;
	}

	public List<String> getAccessLogs() {
		return accessLogs;
	}

	public void setAccessLogs(List<String> accessLogs) {
		this.accessLogs = accessLogs;
	}

	public int getMaxPrefixNum() {
		return maxPrefixNum;
	}

	public int getMaxSuffixNum() {
		return maxSuffixNum;
	}

	public RuleCache getRuleCache() {
		return ruleCache;
	}
}
