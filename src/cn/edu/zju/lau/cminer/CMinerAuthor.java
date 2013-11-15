package cn.edu.zju.lau.cminer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import cn.edu.zju.lau.cminer.model.SubsequenceSuffix;

/**
 * 挖掘序列中事件（字符）的关联关系
 * 实现《C-Miner: Mining Block Correlations in Storage Systems》中所述的C-Miner算法结果，
 * 使用作者的Core Algorithm 生成 Closed Frequent Subsequences。
 * @author yuki lau
 * @date 2013-11-13
 */
public class CMinerAuthor {

	// 存储frequent subsequence在每个segments中的最长suffix
	private Map<String, SubsequenceSuffix> Ds = new HashMap<String,SubsequenceSuffix>(); 
	
	// 存储frequent subsequence
	private Map<String, Integer> freSubsequences = new HashMap<String, Integer>();
	
	/**
	 * 预处理：采用non-overlapped cutting方法将访问序列划分为多个固定长度的短序列片段
	 * 
	 * @param sequence		文件访问序列
	 * @param windowSize 	窗口大小，每个片段的长度
	 * @return 划分完成的短序列片段列表
	 */
	public List<String> cutAccessSequence(String sequence, int windowSize){
		List<String> accessSegments = new ArrayList<String>();
		int start = 0;
		int end = start + windowSize;
		
		while(end < sequence.length()){
			accessSegments.add(sequence.substring(start, end));
			start = end;
			end += windowSize;
		}
		accessSegments.add(sequence.substring(start));
		
		return accessSegments;
	}
	
	public void generateFirstDs(List<String> accessSegments, int minSupport){
		
		Map<String, Integer> charAccessTimes = new HashMap<String, Integer>();
		Queue<String> tempQueue = new LinkedList<String>();  
		
		// 统计每个字符出现的次数，同时记录suffix
		for(int i = 0; i < accessSegments.size();i++){
			String segment = accessSegments.get(i);
			
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
					tempQueue.add(currentChar);
				}
				Ds.get(currentChar).addSuffix(segment.substring(start + 1));
			}
		}
		
		// 过滤掉出现次数小于minSupport的子序列
		while(!tempQueue.isEmpty()){
			String subseq = tempQueue.poll();
			
			if(charAccessTimes.get(subseq) < minSupport){
				Ds.remove(subseq);
			}
			else{
				Ds.get(subseq).setOccurTimes(charAccessTimes.get(subseq));
				Ds.get(subseq).setSubsequence(subseq);
			}
		}
		System.out.println(Ds);
	}
	
	public Set<String> generateOneCharFreSubseq(Collection<String> accessSegments, int minSupport){
		
		Map<String, Integer> charAccessTimes = new HashMap<String, Integer>();
		Set<String> oneCharFreSubseqs = new HashSet<String>();
		
		// 统计每个字符出现的次数
		for(String segment: accessSegments){
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

	/**
	 * 使用DFS方法，产生候选频繁子序列集合（Frequent Subsequences），满足：
	 * 		1）相距不大于maxGap的访问子序列（没必要连续）
	 * 		2）出现次数满足frequent要求，即不小于minSupport
	 * 同时生成每个频繁序列的suffix集合Ds
	 * 
	 * @param accessSegments	完成切分的访问序列片段列表
	 * @param windowSize 	窗口大小，每个片段的长度
	 * @param maxGap		序列中最大的访问间距
	 * @param minSupport	序列最小发生次数
	 * @return	candidate frequent subsequences Map<序列, Support值>
	 */
	public void candidateFreSubsequences(String currentSubseq, int occurTimes, int maxGap, int minSupport){
		
		freSubsequences.put(currentSubseq, occurTimes);
		
		Set<String> currentDs = Ds.get(currentSubseq).getSuffixes();
		
		Set<String> oneCharFreSubseqs = generateOneCharFreSubseq(currentDs, minSupport);
		
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
				
				candidateFreSubsequences(newSeq, newDs.size() + endCount, maxGap, minSupport);
			}
		}
		
		// 处理完毕，从Ds中移除
		Ds.remove(currentSubseq);
		
		// 继续处理下一个
		SubsequenceSuffix nextSeq = getSeqFromDs();
		if(nextSeq == null){
			System.out.println(freSubsequences);
		}
		else{
			candidateFreSubsequences(nextSeq.getSubsequence(), nextSeq.getOccurTimes(), maxGap, minSupport);
		}
		
	}

	public SubsequenceSuffix getSeqFromDs(){
		if(Ds.isEmpty()){
			return null;
		}
		return Ds.entrySet().iterator().next().getValue();
	}
	
	/**
	 * 产生Closed Frequent Subsequences，满足：
	 * 		1. 是候选频繁子序列（Frequent Subsequences）的子集
	 * 		2. 满足Closed条件：与所有super-subsequences的support不同
	 * 
	 * @param subSequences	候选频繁子序列集合（Frequent Subsequences）
	 * @return	Closed Frequent Subsequences，Map<序列, Support值>
	 */
	public Map<String, Integer> closedFreSubsequences(String freSubsequence){
		
		Map<String, Integer> closedSubSequences = new HashMap<String, Integer>();
	
		
		return closedSubSequences;
	}
	
//	Set<String> Ds = new HashSet<String>();
//	Map<String, Integer> mostFrequent = new HashMap<String, Integer>();
//	String mostFreChar = null;
//	int largestNumber = 0;
//	boolean found = false;
//	
//	// 找出第一个可以确定的frequent subsequence
//	for(int i = 0; i < accessSegments.size(); i++){
//		String segment = accessSegments.get(i);
//		
//		// 依次检查分段中的每个字符
//		for(int k = 0; k < segment.length(); k++){
//			String currentChar = segment.charAt(k) + "";
//			
//			// 整个序列的第一个字符
//			if(mostFreChar == null){
//				mostFreChar = currentChar;
//				largestNumber = 1;
//				mostFrequent.put(mostFreChar, largestNumber);
//			}
//			// 非第一个字符，更新当前发生次数最多的字符、最多的发生次数
//			else{
//				if(mostFreChar.equals(currentChar)){
//					largestNumber++;
//					mostFrequent.put(mostFreChar, largestNumber);
//				}
//				else {
//					if(mostFrequent.get(currentChar) == null){
//						mostFrequent.put(currentChar, 1);
//					}
//					else if(mostFrequent.get(currentChar) >= largestNumber){
//						largestNumber = mostFrequent.get(currentChar) + 1;
//						mostFreChar = currentChar;
//						mostFrequent.put(mostFreChar, largestNumber);
//					}
//					else{
//						mostFrequent.put(currentChar, mostFrequent.get(currentChar) + 1);
//					}
//				}
//			}
//			
//			if(largestNumber >= minSupport){
//				found = true;
//				break;
//			}
//		}
//		
//		if(found){
//			break;
//		}
//	}
//	mostFrequent.clear();
//	
//	// 若没有找到任意一个频繁序列，则整个输入集都没有频繁序列
//	if(!found){
//		System.out.println("No frequent subsequence in original sequence with min support " + minSupport);
//		return Ds;
//	}
//	
//	// 为这个频繁序列生成后缀集合Ds
//	for(int i = 0; i < accessSegments.size(); i++){
//		int start = accessSegments.get(i).indexOf(mostFreChar);
//		if(start < 0){
//			continue;
//		}
//		else{
//			Ds.add(accessSegments.get(i).substring(start + 1));
//		}
//	}
//	
//	return Ds;
}
