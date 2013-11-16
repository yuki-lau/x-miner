package cn.edu.zju.lau.cminer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import cn.edu.zju.lau.cminer.model.Rule;

/**
 * CMinerBase，C-Miner算法的抽象类。
 * 包含抽象方法 candidateFreSubsequences，即生成候选频繁子序列的过程是抽象的。
 * 
 * @author yuki
 * @date 2013-11-16
 */
public abstract class CMinerBase {

	protected int windowSize;			// 窗口大小，每个片段的长度
	protected int maxGap;				// 频繁子序列中两个相邻字符（事件）间的最大间隔
	protected int minSupport;			// 频繁序列的最小出现次数
	protected float minConfidence;		// 关联规则的最小confidence，confidence(a->b) = support(b)/support(a)
	protected String inputSequeuece;	// 文件访问序列
	
	protected List<String> inputSegments;					// 文件访问序列的分段，每段长度为windowSize
	protected Map<String, Integer> freSubsequences;			// 候选频繁子序列，对应其出现的次数
	protected Map<String, Integer> closedFreSubsequences;	// Closed频繁子序列
	protected Map<String, Rule> rules;						// 关联规则
	
	// 按候选频繁子序列的长度进行分层，相同长度的子序列存储在同一层中，例如：
	//     freSubsequences: {abc=4, b=4, c=5, a=5, ac=5, ab=4, bc=4}
	//	   freSubsequencesTier：{1={b=4, c=5, a=5}, 2={ac=5, ab=4, bc=4}, 3={abc=4}}
	protected Map<Integer, Map<String, Integer>> freSubsequencesTier;
	protected int maxSeqLength;		// 候选频繁子序列中，最长序列的长度
	
	
	public CMinerBase(){
		
		// 设置默认参数
		windowSize = 5;
		maxGap = windowSize - 2;
		minSupport = 4;
		minConfidence = 1.0F;
		
		// 创建对象
		inputSegments = new ArrayList<String>();
		freSubsequences = new HashMap<String, Integer>();
		closedFreSubsequences = new HashMap<String, Integer>();
		rules = new HashMap<String, Rule>();
		freSubsequencesTier = new HashMap<Integer, Map<String, Integer>>();
		
		maxSeqLength = 0;
	}
	
	public CMinerBase(String inputSequence, int windowSize, int maxGap, int minSupport, float minConfidence){
		
		this.inputSequeuece = inputSequence;
		
		// 设置默认参数
		this.windowSize = windowSize;
		this.maxGap = maxGap;
		this.minSupport = minSupport;
		this.minConfidence = minConfidence;
		
		// 创建对象
		inputSegments = new ArrayList<String>();
		freSubsequences = new HashMap<String, Integer>();
		closedFreSubsequences = new HashMap<String, Integer>();
		rules = new HashMap<String, Rule>();
		freSubsequencesTier = new HashMap<Integer, Map<String, Integer>>();
		
		maxSeqLength = 0;
	}

	/**
	 * 采用non-overlapped cutting方法将访问序列划分为多个固定长度的短序列片段。
	 * 
	 * 生成:	List<String> inputSegments
	 */
	public void cutAccessSequence(){
		
		// 检查输入字符串是否合法
		if(StringUtils.isEmpty(inputSequeuece)){
			System.out.println("Input Sequeuece is null! Exit...");
			return;
		}
		
		// 对输入字符串进行分段
		int start = 0;
		int end = start + windowSize;
		
		while(end < inputSequeuece.length()){
			inputSegments.add(inputSequeuece.substring(start, end));
			start = end;
			end += windowSize;
		}
		inputSegments.add(inputSequeuece.substring(start));
	}
	
	/**
	 * 抽象方法。产生候选频繁子序列集合（Frequent Subsequences），满足：
	 * 		1）相距不大于maxGap的访问子序列（没必要连续）
	 * 		2）出现次数满足frequent要求，即不小于minSupport
	 * 
	 * 生成:	Map<String, Integer> freSubsequences
	 * 		Map<Integer, Map<String, Integer>> freSubsequencesTier;
	 */
	public abstract void candidateFreSubsequences();
	
	/**
	 * 产生Closed Frequent Subsequences，满足：
	 * 		1. 是候选频繁子序列（Frequent Subsequences）的子集
	 * 		2. 满足Closed条件：与所有super-subsequences的support不同
	 * 
	 * 生成:	Map<String, Integer> closedFreSubsequences
	 */
	public void closedFreSubsequences(){

		// 检查候选频繁子序列层次是否为空
		if(freSubsequencesTier == null || freSubsequencesTier.size() == 0){
			System.out.println("Candidate Frequent Sequeueces is null! Exit...");
			return;
		}
		
		// 每层、依次检查每一个frequent subsequence，从中挑选出closed frequent subsequence
		for(int i = this.maxSeqLength; i > 0; i--){
			
			// 最长序列都是closed的
			if(i == this.maxSeqLength){
				for(Map.Entry<String, Integer> entry: this.freSubsequencesTier.get(i).entrySet()){
					closedFreSubsequences.put(entry.getKey(), entry.getValue());
				}
			}
			
			// closed条件：
			// 不是任何frequent subsequence的子序列
			// 或
			// 是子序列 && support 与 大于父序列的support
			else{
				// 依次检查当前层每一个序列
				for(Map.Entry<String, Integer> entry: this.freSubsequencesTier.get(i).entrySet()){
					boolean isSubSubseq = false;
					
					// 当前序列 与 其父层（直接父序列）中每一个序列的关系：是否为子序列、support大小关系
					for(Map.Entry<String, Integer> superEntry: this.freSubsequencesTier.get(i + 1).entrySet()){
						
						// 是子序列
						if(superEntry.getKey().contains(entry.getKey())){
							
							isSubSubseq = true;
							
							// 大于所有父序列的support
							if(entry.getValue() > superEntry.getValue()){
								closedFreSubsequences.put(entry.getKey(), entry.getValue());
								
								// 虽然这一次大于，但是有可能是有小于等于的父序列的，所以需要检测完
								continue;
							}
							else{
								closedFreSubsequences.remove(entry.getKey());
								
								// 一票否定，无需继续检测
								break;
							}
						}
					}
					
					// 不是任何frequent subsequence的子序列
					if(!isSubSubseq){
						closedFreSubsequences.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
	}

	/**
	 * 生成关联规则，满足：
	 * 		1. 规则格式：子序列（长度>=1） -> 后续子序列（长度=1）
	 * 		2. 从Closed Frequent Subsequences中生成
	 * 		3. 每个Rule的confidence不小于minConfidence
	 * 		4. 多个Closed Frequent Subsequences产生相同的rule，取最大support作为rule的support
	 * 
	 * 生成:	Map<String, Rule> rules
	 */
	public Map<String, Rule> generateRules(){
		
		// 检查候选频繁子序列、Closed频繁子序列是否为空
		if(freSubsequences == null || freSubsequences.size() == 0){
			System.out.println("Candidate Frequent Sequeueces is null! Exit...");
			return rules;
		}
		if(closedFreSubsequences == null || closedFreSubsequences.size() == 0){
			System.out.println("Closed Frequent Sequeueces is null! Exit...");
			return rules;
		}
		
		// 依次处理每一个closed frequent subsequence，获取rules
		for(Map.Entry<String, Integer> closedEntry: closedFreSubsequences.entrySet()){
			
			String closedSeq = closedEntry.getKey();
			int closedSeqConf = closedEntry.getValue();
			
			// 只有一个字符序列，无法导出关系，跳过
			if(closedSeq.length() <= 1){
				continue;
			}

			// 开始生成rule
			for(int historyStart = 0; historyStart < closedSeq.length() - 1; historyStart++){
			
				// 生成history子序列
				for(int historyEnd = historyStart + 1; historyEnd < closedSeq.length(); historyEnd++){
				
					String history = closedSeq.substring(historyStart, historyEnd);
					float historyConf = freSubsequences.get(history) * 1.0F;
				
					// 生成prediction子序列（只有一个字符）
					for(int predictionStart = historyEnd; predictionStart < closedSeq.length(); predictionStart++){
						
						String prediction = closedSeq.substring(predictionStart, predictionStart + 1);
						float newRuleConf = freSubsequences.get(prediction) / historyConf;
						
						// 当前规则confidence不够，跳过
						if(newRuleConf < minConfidence){
							continue;
						}
						
						// 关联规则生成成功，放入规则集合中
						String ruleStr = history + "|" + prediction;
						if(rules.get(ruleStr) == null){
							rules.put(ruleStr, new Rule(history, prediction, closedSeqConf, newRuleConf));
						}
						else{
							if(rules.get(ruleStr).getSupport() < closedSeqConf){
								rules.get(ruleStr).setSupport(closedSeqConf);
							}
						}
					}
				}
			}
		}
		
		return rules;
	}
	
	/**
	 * 执行关联规则的挖掘过程
	 * 
	 * @return rules
	 */
	public Map<String, Rule> startMining(){

		// 对初始访问序列分段
		cutAccessSequence();
		
		// 挖掘：频繁子序列
		candidateFreSubsequences();
		
		// 过滤：Closed频繁子序列
		closedFreSubsequences();
		
		// 生成：关联规则
		generateRules();
		
		return rules;
	}
	
	/**
	 * 清除List/MAP对象占用的空间，恢复初始状态
	 */
	public void clear(){
		inputSegments.clear();
		freSubsequences.clear();
		closedFreSubsequences.clear();
		freSubsequencesTier.clear();
		rules.clear();
		
		maxSeqLength = 0;
	}

	
	
	/* 算法参数的 getters and setters */
	
	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public int getMaxGap() {
		return maxGap;
	}

	public void setMaxGap(int maxGap) {
		this.maxGap = maxGap;
	}

	public int getMinSupport() {
		return minSupport;
	}

	public void setMinSupport(int minSupport) {
		this.minSupport = minSupport;
	}

	public float getMinConfidence() {
		return minConfidence;
	}

	public void setMinConfidence(float minConfidence) {
		this.minConfidence = minConfidence;
	}

	public String getInputSequeuece() {
		return inputSequeuece;
	}

	public void setInputSequeuece(String inputSequeuece) {
		this.inputSequeuece = inputSequeuece;
	}

	
	/* 中间结果的getters */
	
	public List<String> getInputSegments() {
		return inputSegments;
	}

	public Map<String, Integer> getFreSubsequences() {
		return freSubsequences;
	}

	public Map<String, Integer> getClosedFreSubsequences() {
		return closedFreSubsequences;
	}

	public Map<String, Rule> getRules() {
		return rules;
	}

	public Map<Integer, Map<String, Integer>> getFreSubsequencesTier() {
		return freSubsequencesTier;
	}

	public int getMaxSeqLength() {
		return maxSeqLength;
	}
	
	
	/* 覆盖、重写（Overriding）toString */
	public String toString(){
		StringBuilder sb = new StringBuilder("\n======== Generating Correlation Rules ========\n");
		
		sb.append("Window size:\t").append(this.windowSize).append("\n")
		  .append("Max Gap:\t").append(this.maxGap).append("\n")
		  .append("Min Support:\t").append(this.minSupport).append("\n")
		  .append("Min Confidence:\t").append(this.minConfidence).append("\n");
		
		sb.append("Input Sequence Length:\t\t").append(this.inputSequeuece.length()).append("\n")
		  .append("Input Segments Length:\t\t").append(this.inputSegments.size()).append("\n")
		  .append("Frequent Subsequences:\t\t").append(this.freSubsequences).append("\n")
		  .append("Closed Frequent Subsequences:\t").append(this.closedFreSubsequences).append("\n");

		sb.append("Rules Number:\t").append(rules.size()).append("\n");
		
		return sb.toString();
	}
}
