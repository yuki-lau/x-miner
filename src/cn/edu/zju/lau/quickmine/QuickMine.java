package cn.edu.zju.lau.quickmine;

import java.util.ArrayList;
import java.util.List;

import cn.edu.zju.lau.quickmine.model.RuleCache;
import cn.edu.zju.lau.quickmine.model.Suffix;
import cn.edu.zju.lau.quickmine.model.SuffixList;

/**
 * 实现论文《Context-Aware Prefetching at the Storage Server》中阐述的挖掘Block访问关联性的方法。
 * @author yuki
 * @date 2013-11-26
 */
public class QuickMine {
	
	private int maxGap;					// 关联序列间的最大间隔为maxGap j - i <= maxGap
	private int maxPrefixNum;			// Rule Cache中对多的prefix数量
	private int maxSuffixNum;			// Rule Cache中每个prefix对应的最多的suffix数量
	private List<String> accessLogs;  	// 待挖掘的访问序列
	private List<String> currentLogs;	// on-the-fly生成规则的过程中，需要保存的日志
	private RuleCache ruleCache;		// 存放生成的关联规则
	
	/**
	 * maxPrefixNum和maxSuffixNum必须在构造时指定，否则使用默认值，没有setters，因为需要用来创建RuleCache。
	 */
	public QuickMine(){
		this.maxGap = 5;
		this.maxPrefixNum = 1024;
		this.maxSuffixNum = 16;
		this.accessLogs = new ArrayList<String>();
		this.currentLogs = new ArrayList<String>();
		this.ruleCache = new RuleCache(this.maxPrefixNum, this.maxSuffixNum);
	}
	
	public QuickMine(int maxPrefixNum, int maxSuffixNum){
		this.maxPrefixNum = maxPrefixNum;
		this.maxSuffixNum = maxSuffixNum;
		this.maxGap = 5;
		this.accessLogs = new ArrayList<String>();
		this.currentLogs = new ArrayList<String>();
		this.ruleCache = new RuleCache(this.maxPrefixNum, this.maxSuffixNum);
	}
	
	public QuickMine(int maxPrefixNum, int maxSuffixNum, int maxGap, int prefetchNum){
		this.maxPrefixNum = maxPrefixNum;
		this.maxSuffixNum = maxSuffixNum;
		this.maxGap = maxGap;
		this.accessLogs = new ArrayList<String>();
		this.currentLogs = new ArrayList<String>();
		this.ruleCache = new RuleCache(this.maxPrefixNum, this.maxSuffixNum);
	}

	/**
	 * 批量挖掘关联规则：根据所有的访问日志，生成所有的规则。
	 */
	public void miningByBatch(){
		
		// 检查输入日志序列
		if(accessLogs == null || accessLogs.size() == 0){
			System.err.println("Access Logs is null! Exit...");
			return;
		}
		
		// 根据规则 Ai & Aj -> Ak, i < j < k, j - i <= maxGap, k - j <= maxGap 生成规则
		for(int i = 0; i < accessLogs.size() - 2; i++){
			
			for(int j = i + 1; j <= maxGap + i && j < accessLogs.size() - 1; j++){
				
				String prefix = accessLogs.get(i) + "|" + accessLogs.get(j);
				
				for(int k = j + 1; k <= maxGap + j && k < accessLogs.size(); k++){
					
					String suffix = accessLogs.get(k);
					ruleCache.addRule(prefix, suffix);
				}
			}
		}
	}
	
	/**
	 * 增量挖掘关联规则: 添加一个新的访问记录，同时生成新rule。保证线程同步。
	 * 
	 * @param log
	 */
	public synchronized void miningByStep(String log){
		
		int maxSize = 2 * maxGap + 1;
				
		// 如果currentLogs.size < maxSize, 则直接添加新log
		if(currentLogs.size() < maxSize){
			currentLogs.add(log);
		}
		// 如果currentLogs.size >= maxSize, 则需要剔除最老的log，再添加新log
		else{
			currentLogs.remove(0);
			currentLogs.add(log);
		}
		
		// 根据规则 Ai & Aj -> Ak, i < j < k, j - i <= maxGap, k - j <= maxGap 生成新规则
		// 从后向前，确定maxGap的区间找prefix
		int lastPos = currentLogs.size() - 1;
		for(int j = lastPos - 1; j >= lastPos - maxGap && j >= 1; j --){
		
			for(int i = j - 1; i >= j - maxGap && i >= 0; i--){
				String prefix = currentLogs.get(i) + "|" + currentLogs.get(j);
				ruleCache.addRule(prefix, log);
			}
		}
	}
	
	/**
	 * 根据prefix返回预测的后缀列表
	 * @param prefix
	 * @return
	 */
	public List<Suffix> getPredictSuffix(String prefix){
		
		List<Suffix> suffixList = new ArrayList<Suffix>();
		SuffixList suffixes = ruleCache.getRules().get(prefix);
		
		if(suffixes != null){
			suffixList.addAll(suffixes.getSuffixList());
		}
		
		return suffixList;
	}
	
	/* getters and setters */
	
	public int getMaxGap() {
		return maxGap;
	}

	public void setMaxGap(int maxGap) {
		this.maxGap = maxGap;
	}

	public List<String> getAccessLogs() {
		return accessLogs;
	}

	public void setAccessLogs(List<String> accessLogs) {
		this.accessLogs = accessLogs;
	}

	public List<String> getCurrentLogs() {
		return currentLogs;
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

	
	/* test */
	public static void main(String[] args){
		
		// 测试数据
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
		
		QuickMine miner = new QuickMine();
		miner.setMaxGap(3);
		
		// 批量挖掘关联规则
//		miner.setAccessLogs(logs);
//		miner.miningByBatch();
		
		// 增量挖掘关联规则
		for(int i = 0; i < logs.size(); i++){
			miner.miningByStep(logs.get(i));		
		}
		
		System.out.println(miner.getRuleCache());	
	}
}
