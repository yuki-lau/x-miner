package cn.edu.zju.lau.cminer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import cn.edu.zju.lau.cminer.model.FileAccessLog;
import cn.edu.zju.lau.cminer.model.HDFSRule;
import cn.edu.zju.lau.cminer.model.HDFSSubseqSuffix;

/**
 * 挖掘HDFS Audit Log中文件访问的关联关系
 * 采用C-Miner算法，使用DFS算法生成候选频繁子序列。
 * 
 * @author yuki lau
 * @date 2013-11-22
 */

public class CMinerHDFS{
	
	protected int windowSize;			// 窗口大小，每个片段的长度
	protected int maxGap;				// 频繁子序列中两个相邻字符（事件）间的最大间隔
	protected int minSupport;			// 频繁序列的最小出现次数
	protected float minConfidence;		// 关联规则的最小confidence，confidence(a->b) = support(b)/support(a)
	
	protected List<FileAccessLog> inputSequence;			// 文件访问序列
	protected List<List<FileAccessLog>> inputSegments;		// 文件访问序列的分段，每段长度为windowSize
	protected Map<String, Integer> freSubsequences;			// 候选频繁子序列，对应其出现的次数
	protected Map<String, Integer> closedFreSubsequences;	// Closed频繁子序列
	protected Map<String, HDFSRule> rules;					// 关联规则
	protected Map<String, HDFSSubseqSuffix> Ds;				// 存储frequent subsequence在每个segments中的最长suffix
	
	// 按候选频繁子序列的长度进行分层，相同长度的子序列存储在同一层中，例如：
	//     freSubsequences: {abc=4, b=4, c=5, a=5, ac=5, ab=4, bc=4}
	//	   freSubsequencesTier：{1={b=4, c=5, a=5}, 2={ac=5, ab=4, bc=4}, 3={abc=4}}
	protected Map<Integer, Map<String, Integer>> freSubsequencesTier;
	protected int maxSeqLength;		// 候选频繁子序列中，最长序列的长度

	
	public CMinerHDFS(){
		
		// 设置默认参数
		windowSize = 5;
		maxGap = windowSize - 2;
		minSupport = 4;
		minConfidence = 1.0F;
		
		// 创建对象
		inputSequence = new ArrayList<FileAccessLog>();
		inputSegments = new ArrayList<List<FileAccessLog>>();
		freSubsequences = new HashMap<String, Integer>();
		closedFreSubsequences = new HashMap<String, Integer>();
		rules = new HashMap<String, HDFSRule>();
		freSubsequencesTier = new HashMap<Integer, Map<String, Integer>>();
		Ds = new HashMap<String, HDFSSubseqSuffix>();
		
		maxSeqLength = 0;
	}
	
	public CMinerHDFS(List<FileAccessLog> inputSequence, int windowSize, int maxGap, int minSupport, float minConfidence){
		
		this.inputSequence = inputSequence;
		
		// 设置默认参数
		this.windowSize = windowSize;
		this.maxGap = maxGap;
		this.minSupport = minSupport;
		this.minConfidence = minConfidence;
		
		// 创建对象
		inputSegments = new ArrayList<List<FileAccessLog>>();
		freSubsequences = new HashMap<String, Integer>();
		closedFreSubsequences = new HashMap<String, Integer>();
		rules = new HashMap<String, HDFSRule>();
		freSubsequencesTier = new HashMap<Integer, Map<String, Integer>>();
		Ds = new HashMap<String, HDFSSubseqSuffix>();
		
		maxSeqLength = 0;
	}

	/**
	 * 采用non-overlapped cutting方法将访问序列划分为多个固定长度的短序列片段。
	 * 
	 * 生成:	List<List<FileAccessLog>> inputSegments
	 */
	public void cutAccessSequence(){
		
		// 检查输入日志序列
		if(inputSequence == null || inputSequence.size() == 0){
			System.err.println("Input Sequeuece is null! Exit...");
			return;
		}
		
		// 对输入日志序列进行分段
		int winNum = -1;
		for(int i = 0; i < inputSequence.size(); i++){
			
			// 开始一个新窗口
			if(i / windowSize > winNum){
				List<FileAccessLog> newWindow = new ArrayList<FileAccessLog>();
				newWindow.add(inputSequence.get(i));
				inputSegments.add(newWindow);
				winNum = i / windowSize;
			}
			// 在旧窗口中追加访问项
			else{
				inputSegments.get(winNum).add(inputSequence.get(i));
			}
		}
	}
	
	/**
	 * 获取长度为1的频繁序列，以及各个频繁子序列的后缀集合
	 * 
	 * 生成:	Map<String, HDFSSubseqSuffix> Ds
	 */
	public void generateFirstDs(){
		
		Map<String, Integer> fileAccessTimes = new HashMap<String, Integer>();
		
		// 统计每个文件访问的次数，同时记录suffix
		for(int i = 0; i < inputSegments.size();i++){
			List<FileAccessLog> segment = inputSegments.get(i);
			
			for(int k = 0; k < segment.size(); k++){
				String currentFile = segment.get(k).getSrc();
				
				// 统计每个文件出现的次数
				Integer count = fileAccessTimes.get(currentFile) == null ? 0 : fileAccessTimes.get(currentFile);
				fileAccessTimes.put(currentFile, count + 1);
				
				// 判断当前文件在当前窗口中是否被统计过
				int start = 0;
				for(; start < segment.size() && start < k; start++){
					if(currentFile.equalsIgnoreCase(segment.get(start).getSrc())){
						break;
					}
				}
				if(start != k || start == segment.size() - 1){
					continue;
				}
				
				// 记录suffix
				StringBuilder suffix = new StringBuilder();
				for(int j = start + 1; j < segment.size(); j++){
					suffix.append(segment.get(j).getSrc()).append("|");
				}
				if(Ds.get(currentFile) == null){
					Ds.put(currentFile, new HDFSSubseqSuffix());
				}
				Ds.get(currentFile).addSuffix(suffix.toString());
			}
		}
		
		// 过滤掉出现次数小于minSupport的子序列
		for(Map.Entry<String, Integer> entry: fileAccessTimes.entrySet()){
			if(entry.getValue() < minSupport){
				Ds.remove(entry.getKey());
			}
			else{
				Ds.get(entry.getKey()).setOccurTimes(entry.getValue());
				Ds.get(entry.getKey()).setSubsequence(entry.getKey());
			}
		}
	}
	
	/**
	 * 从Ds集合中挑选出一个子序列，作为处理使用
	 */
	public HDFSSubseqSuffix getSeqFromDs(){
		if(Ds.isEmpty()){
			return null;
		}
		return Ds.entrySet().iterator().next().getValue();
	}
	
	/**
	 * DFS 产生候选频繁子序列集合（Frequent Subsequences），满足：
	 * 		1）相距不大于maxGap的访问子序列（没必要连续）
	 * 		2）出现次数满足frequent要求，即不小于minSupport
	 * 
	 * 生成:	Map<String, Integer> freSubsequences
	 * 		Map<Integer, Map<String, Integer>> freSubsequencesTier;
	 */
	public void candidateFreSubsequences(String currentSubseq, int occurTimes){
		
		// 添加当前序列至 候选频繁子序列集合中
		freSubsequences.put(currentSubseq, occurTimes);
		
		// 添加当前序列至 候选频发子序列对应的长度层次中
		int seqLen = currentSubseq.split("\\|").length;
		if(seqLen > maxSeqLength){
			maxSeqLength = seqLen;
		}
		if(freSubsequencesTier.get(seqLen) == null){
			freSubsequencesTier.put(seqLen, new HashMap<String, Integer>());
		}
		freSubsequencesTier.get(seqLen).put(currentSubseq, occurTimes);
		
		// 获取当前序列的后缀集合
		Set<String> currentDs = Ds.get(currentSubseq).getSuffixes();
		
		// 从当前的后缀集合中计算出只包含一个访问序列的频繁子序列集合
		Set<String> oneFileFreSubseqs = generateOneCharFreSubseq(currentDs);
		
		// 一次扫描每一个后缀集合中 只包含1个文件的频繁子序列
		for(String file: oneFileFreSubseqs){
			
			// 类似AA这种不检测
			if(currentSubseq.equalsIgnoreCase(file)){
				continue;
			}
			
			// 检测 currentSubseq连接file是否为frequent subsequence，同时，记录file有效出现时的新suffix
			String newSeq = currentSubseq + "|" + file;
			Set<String> newDs = new HashSet<String>();
			int endCount = 0;
			
			for(String suffix: currentDs){
				if(suffix.contains(file)){
					String[] suffixFiles = suffix.split("\\|");
					
					for(int i = 0; i < suffixFiles.length && i <= maxGap; i++){
						if(i == suffixFiles.length - 1){
							endCount++;
						}
						else if(suffixFiles[i].equalsIgnoreCase(file)){
							newDs.add(suffix.substring(suffix.indexOf(suffixFiles[i + 1])));
							break;
						}
					}
				}
			}
			
			// 对于达到minSupport的新序列，递归调用
			if(newDs.size() + endCount >= minSupport){
				Ds.put(newSeq, new HDFSSubseqSuffix(newSeq, newDs.size() + endCount, newDs));
				
				// 处理完毕，从Ds中移除
				Ds.remove(currentSubseq);
				
				candidateFreSubsequences(newSeq, newDs.size() + endCount);
			}
		}
		
		// 处理完毕，从Ds中移除
		Ds.remove(currentSubseq);
		
		// 继续处理下一个
		HDFSSubseqSuffix nextSeq = getSeqFromDs();
		if(nextSeq != null){
			candidateFreSubsequences(nextSeq.getSubsequence(), nextSeq.getOccurTimes());
		}
	}

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
			System.err.println("Candidate Frequent Sequeueces is null! Exit...");
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
						if(superEntry.getKey().endsWith("|" + entry.getKey())
							|| superEntry.getKey().startsWith(entry.getKey() + "|")
							|| superEntry.getKey().contains("|" + entry.getKey() + "|")){
							
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
	 * 生成:	Map<String, HDFSRule> rules
	 */
	public Map<String, HDFSRule> generateRules(){
		
		// 检查候选频繁子序列、Closed频繁子序列是否为空
		if(freSubsequences == null || freSubsequences.size() == 0){
			System.err.println("Candidate Frequent Sequeueces is null! Exit...");
			return rules;
		}
		if(closedFreSubsequences == null || closedFreSubsequences.size() == 0){
			System.err.println("Closed Frequent Sequeueces is null! Exit...");
			return rules;
		}
		
		// 依次处理每一个closed frequent subsequence，获取rules
		for(Map.Entry<String, Integer> closedEntry: closedFreSubsequences.entrySet()){
			
			String closedSeq = closedEntry.getKey();
			int closedSeqConf = closedEntry.getValue();
			String[] accessFiles = closedSeq.split("\\|");
			
			// 只有一个文件的序列，无法导出关系，跳过
			if(accessFiles.length <= 1){
				continue;
			}

			// 开始生成rule
			for(int historyStart = 0; historyStart < accessFiles.length - 1; historyStart++){
			
				// 生成history子序列
				for(int historyEnd = historyStart + 1; historyEnd < accessFiles.length; historyEnd++){
				
					List<String> historyList = new ArrayList<String>();
					for(int i = historyStart; i < historyEnd; i++){
						historyList.add(accessFiles[i]);
					}
					String historyStr = StringUtils.join(historyList.iterator(), "|");
					float historyConf = freSubsequences.get(historyStr) * 1.0F;
				
					// 生成prediction子序列（只有一个文件）
					for(int predictionStart = historyEnd; predictionStart < accessFiles.length; predictionStart++){
						
						String prediction = accessFiles[predictionStart];
						float newRuleConf = freSubsequences.get(prediction) / historyConf;
						
						// 当前规则confidence不够，跳过
						if(newRuleConf < minConfidence){
							continue;
						}
						
						// 关联规则生成成功，放入规则集合中
						// String ruleStr = historyStr + "->" + prediction;
						String ruleStr = historyStr;
						if(rules.get(ruleStr) == null){
							rules.put(ruleStr, new HDFSRule(historyList, prediction, closedSeqConf, newRuleConf));
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
	 * 从输入的segments中计算出只包含一个访问的频繁子序列集合，并返回
	 */
	private Set<String> generateOneCharFreSubseq(Collection<String> segments){
		
		Map<String, Integer> fileAccessTimes = new HashMap<String, Integer>();
		Set<String> oneFileFreSubseqs = new HashSet<String>();
		
		// 统计每个文件出现的次数
		for(String segment: segments){
			String[] files = segment.split("\\|");
			for(String file: files){
				// 统计每个文件出现的次数
				Integer count = fileAccessTimes.get(file) == null ? 0 : fileAccessTimes.get(file);
				fileAccessTimes.put(file, count + 1);
			}
		}
		
		// 过滤掉出现次数小于minSupport的子序列
		for(Map.Entry<String, Integer> entry: fileAccessTimes.entrySet()){
			if(entry.getValue() >= minSupport){
				oneFileFreSubseqs.add(entry.getKey());
			}
		}
		
		return oneFileFreSubseqs;
	}
	
	/**
	 * 清除List/MAP对象占用的空间，恢复初始状态
	 */
	public void clear(){
		inputSequence.clear();
		inputSegments.clear();
		freSubsequences.clear();
		closedFreSubsequences.clear();
		freSubsequencesTier.clear();
		rules.clear();
		Ds.clear();
		
		maxSeqLength = 0;
	}
	
	
	/**
	 * 执行关联规则的挖掘过程
	 * 
	 * @return rules
	 */
	public Map<String, HDFSRule> startMining() {
		
		// 设置输入
		setInputSequence(inputSequence);
		
		// 对初始访问序列分段
		cutAccessSequence();
		
		// 获取长度为1的频繁序列
		generateFirstDs();
				
		// 挖掘：频繁子序列
		HDFSSubseqSuffix ss = getSeqFromDs();
		candidateFreSubsequences(ss.getSubsequence(), ss.getOccurTimes());

		// 过滤：Closed频繁子序列
		closedFreSubsequences();
		
		// 生成：关联规则
		generateRules();
		
		return rules;
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

	public List<FileAccessLog> getInputSequence() {
		return inputSequence;
	}

	public void setInputSequence(List<FileAccessLog> inputSequence) {
		this.inputSequence.addAll(inputSequence);
	}

	
	/* 中间结果的getters */
	
	public List<List<FileAccessLog>> getInputSegments() {
		return inputSegments;
	}

	public Map<String, HDFSSubseqSuffix> getDs(){
		return Ds;
	}
	
	public Map<String, Integer> getFreSubsequences() {
		return freSubsequences;
	}

	public Map<String, Integer> getClosedFreSubsequences() {
		return closedFreSubsequences;
	}

	public Map<String, HDFSRule> getRules() {
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
		
		sb.append("Input Sequence Length:\t\t").append(this.inputSequence.size()).append("\n")
		  .append("Input Segments Length:\t\t").append(this.inputSegments.size()).append("\n")
		  .append("Frequent Subsequences:\t\t").append(this.freSubsequences).append("\n")
		  .append("Closed Frequent Subsequences:\t").append(this.closedFreSubsequences).append("\n");

		sb.append("Rules Number:\t").append(rules.size()).append("\n");
		
		return sb.toString();
	}
}
