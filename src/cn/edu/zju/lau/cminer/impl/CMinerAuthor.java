package cn.edu.zju.lau.cminer.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.edu.zju.lau.cminer.CMinerBase;
import cn.edu.zju.lau.cminer.model.Rule;
import cn.edu.zju.lau.cminer.model.SubsequenceSuffix;

/**
 * 挖掘序列中事件（字符）的关联关系
 * 实现《C-Miner: Mining Block Correlations in Storage Systems》中所述的C-Miner算法结果，
 * 使用作者的Core Algorithm 生成 Candidate Frequent Subsequences：DFS算法。
 * 
 * @author yuki lau
 * @date 2013-11-16
 */

public class CMinerAuthor extends CMinerBase {

	// 存储frequent subsequence在每个segments中的最长suffix
	private Map<String, SubsequenceSuffix> Ds;
	
	public CMinerAuthor(){
		super();
		Ds = new HashMap<String,SubsequenceSuffix>();
	}
	
	public CMinerAuthor(String inputSequence, int windowSize, int maxGap, int minSupport, float minConfidence){
		super(inputSequence, windowSize, maxGap, minSupport, minConfidence);
		Ds = new HashMap<String,SubsequenceSuffix>();
	}
	
	@Override
	public void candidateFreSubsequences() {
		System.out.println("生成候选频繁子序列的DFS方法需要参数，抽象接口没定义好，哎~ ");
		System.out.println("请调用有参数的同名方法。");
	}
	
	@Override
	public Map<String, Rule> startMining() {
		// 对初始访问序列分段
		cutAccessSequence();
		
		// 获取长度为1的频繁序列
		generateFirstDs();
				
		// 挖掘：频繁子序列
		SubsequenceSuffix ss = getSeqFromDs();
		candidateFreSubsequences(ss.getSubsequence(), ss.getOccurTimes());

		// 过滤：Closed频繁子序列
		closedFreSubsequences();
		
		// 生成：关联规则
		generateRules();
		
		return rules;
	}
	


	@Override
	public void clear() {
		super.clear();
		Ds.clear();
	}
	

	/**
	 * 获取长度为1的频繁序列，以及各个频繁子序列的后缀集合
	 * 
	 * 生成:	Map<String, SubsequenceSuffix> Ds
	 */
	public void generateFirstDs(){
		
		Map<String, Integer> charAccessTimes = new HashMap<String, Integer>();
		
		// 统计每个字符出现的次数，同时记录suffix
		for(int i = 0; i < inputSegments.size();i++){
			String segment = inputSegments.get(i);
			
			for(int k = 0; k < segment.length(); k++){
				String currentChar = segment.substring(k, k + 1);
				
				// 统计每个字符出现的次数
				Integer count = charAccessTimes.get(currentChar) == null ? 0 : charAccessTimes.get(currentChar);
				charAccessTimes.put(currentChar, count + 1);
				
				// 记录suffix，每个字符只记录一次，防止重复
				int start = segment.indexOf(currentChar);
				if(start != k || start == segment.length() - 1){
					continue;
				}
				if(Ds.get(currentChar) == null){
					Ds.put(currentChar, new SubsequenceSuffix());
				}
				Ds.get(currentChar).addSuffix(segment.substring(start + 1));
			}
		}
		
		// 过滤掉出现次数小于minSupport的子序列
		for(Map.Entry<String, Integer> entry: charAccessTimes.entrySet()){
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
	 * 抽象方法。DFS 产生候选频繁子序列集合（Frequent Subsequences），满足：
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
		int seqLen = currentSubseq.length();
		if(seqLen > maxSeqLength){
			maxSeqLength = seqLen;
		}
		if(freSubsequencesTier.get(seqLen) == null){
			freSubsequencesTier.put(seqLen, new HashMap<String, Integer>());
		}
		freSubsequencesTier.get(seqLen).put(currentSubseq, occurTimes);
		
		// 获取当前序列的后缀集合
		Set<String> currentDs = Ds.get(currentSubseq).getSuffixes();
		
		// 从当前的后缀集合中计算出一个字符长度的频繁子序列集合
		Set<String> oneCharFreSubseqs = generateOneCharFreSubseq(currentDs);
		
		// 一次扫描每一个后缀集合中 长度为1的频繁子序列
		for(String alpha: oneCharFreSubseqs){
			
			// 类似AA这种不检测
			if(currentSubseq.endsWith(alpha)){
				continue;
			}
			
			// 检测 currentSubseq连接alpha是否为frequent subsequence，同时，记录alpha有效出现时的新suffix
			String newSeq = currentSubseq + alpha;
			Set<String> newDs = new HashSet<String>();
			int endCount = 0;
			for(String suffix: currentDs){
				int position = suffix.indexOf(alpha);
				if(position == suffix.length() - 1){
					endCount++;
				}
				else if(position >= 0 && position <= maxGap){
					newDs.add(suffix.substring(position + 1));
				}
			}
			
			// 对于达到minSupport的新序列，递归调用
			if(newDs.size() + endCount >= minSupport){
				Ds.put(newSeq, new SubsequenceSuffix(newSeq, newDs.size() + endCount, newDs));
				
				// 处理完毕，从Ds中移除
				Ds.remove(currentSubseq);
				
				candidateFreSubsequences(newSeq, newDs.size() + endCount);
			}
		}
		
		// 处理完毕，从Ds中移除
		Ds.remove(currentSubseq);
		
		// 继续处理下一个
		SubsequenceSuffix nextSeq = getSeqFromDs();
		if(nextSeq != null){
			candidateFreSubsequences(nextSeq.getSubsequence(), nextSeq.getOccurTimes());
		}
	}
	
	/**
	 * 从Ds集合中挑选出一个子序列，作为处理使用
	 */
	public SubsequenceSuffix getSeqFromDs(){
		if(Ds.isEmpty()){
			return null;
		}
		return Ds.entrySet().iterator().next().getValue();
	}
	
	/**
	 * 从输入的segments中计算出长度为1的频繁子序列集合，并返回
	 */
	private Set<String> generateOneCharFreSubseq(Collection<String> segments){
		
		Map<String, Integer> charAccessTimes = new HashMap<String, Integer>();
		Set<String> oneCharFreSubseqs = new HashSet<String>();
		
		// 统计每个字符出现的次数
		for(String segment: segments){
			for(int k = 0; k < segment.length(); k++){
				String currentChar = segment.substring(k, k + 1);
				
				// 统计每个字符出现的次数
				Integer count = charAccessTimes.get(currentChar) == null ? 0 : charAccessTimes.get(currentChar);
				charAccessTimes.put(currentChar, count + 1);
			}
		}
		
		// 过滤掉出现次数小于minSupport的子序列
		for(Map.Entry<String, Integer> entry: charAccessTimes.entrySet()){
			if(entry.getValue() >= minSupport){
				oneCharFreSubseqs.add(entry.getKey());
			}
		}
		
		return oneCharFreSubseqs;
	}

}
