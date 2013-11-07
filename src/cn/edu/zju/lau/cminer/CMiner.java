package cn.edu.zju.lau.cminer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.edu.zju.lau.cminer.model.Rule;

/**
 * 挖掘序列中事件（字符）的关联关系
 * 实现《C-Miner: Mining Block Correlations in Storage Systems》中所述的C-Miner算法结果，但具体生成算法不同
 * @author yuki lau
 * @date 2013-11-06
 */
public class CMiner {
	
	// 存储不同长度的frequent subsequence，Map<长度, Map<序列, Support值>>，例如 ：
	//     frequent subsequence: {abc=4, b=4, c=5, a=5, ac=5, ab=4, bc=4}
	//	   Subsequence Tier：{1={b=4, c=5, a=5}, 2={ac=5, ab=4, bc=4}, 3={abc=4}}
	private Map<Integer, Map<String, Integer>> subsequenceTier = new HashMap<Integer, Map<String, Integer>>();
	private int maxSeqLength = 0;
	
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
	
	/**
	 * 产生候选频繁子序列集合（Frequent Subsequences），满足：
	 * 		1）相距不大于maxGap的访问子序列（没必要连续）
	 * 		2）出现次数满足frequent要求，即不小于minSupport
	 * 
	 * @param accessSegments	完成切分的访问序列片段列表
	 * @param windowSize 	窗口大小，每个片段的长度
	 * @param maxGap		序列中最大的访问间距
	 * @param minSupport	序列最小发生次数
	 * @return	candidate frequent subsequences Map<序列, Support值>
	 */
	public Map<String, Integer> candidateFreSubsequences(List<String> accessSegments, int windowSize, int maxGap, int minSupport){
		
		Map<String, Integer> subSequences = new HashMap<String, Integer>();
		Map<String, Integer> newSequences = new HashMap<String, Integer>();
		int currentSeqLen = 0;
		
		// 子序列长度没有限制，因此最大可以为windowSize，因此执行windowSize轮扫描
		while(++currentSeqLen <= windowSize){

			// 挖掘关联子序列
			for(String segment: accessSegments){
				// 初始第一轮扫描，获取最基础（每个字符）的出现次数
				if(1 == currentSeqLen){
					for(int i = 0; i < segment.length(); i++){
						String oneCharStr = segment.substring(i, i + 1);
						Integer count = newSequences.get(oneCharStr) == null ? 0 : newSequences.get(oneCharStr);
						newSequences.put(oneCharStr, count + 1);
					}
				}
				
				// length > 1的序列挖掘
				else{
					for(String prefix: subSequences.keySet()){
						
						// 序列的每次扫描长度 = 当前最长序列 长度+ 1
						if(prefix.length() < currentSeqLen - 1){
							continue;
						}
						
						// 这个POS的判断：
						// 1. 包含该prefix中的所有字符即可
						// 2. POS的值为prefix中最后一个字符在segment中的后一个位置
						int pos = -1;
						for(char c: prefix.toCharArray()){
							pos = segment.indexOf(c);
							if(pos < 0){
								break;
							}
						}
						if(pos < 0){
							continue;
						}
						
						// 以prefix为关联序列开始，在segment中继续延伸关联序列
						for(int i = pos + 1; i < segment.length() && (i - pos - 1) <= maxGap; i++){
							String oneCharStr = segment.substring(i, i + 1);
							if(prefix.contains(oneCharStr)){
								continue;
							}

							String newSeq = prefix + oneCharStr;
							Integer count = newSequences.get(newSeq) == null ? 0 : newSequences.get(newSeq);
							newSequences.put(newSeq, count + 1);
						}
					}
				}
			}
			
			// 去除support < minSupport的序列
			Iterator<Map.Entry<String, Integer>> it = newSequences.entrySet().iterator();  
	        while(it.hasNext()){  
	            if(it.next().getValue() < minSupport){
					it.remove();
				} 
	        }  
	        
	        // 将newSequences添加到subSequences中
	        for(Map.Entry<String, Integer> entry: newSequences.entrySet()){
	        	
	        	subSequences.put(entry.getKey(), entry.getValue());
	        	
	        	// 对得到的frequent subsequence根据subsequence的长度进行分组
	        	int keyLength = entry.getKey().length();
	        	if(subsequenceTier.containsKey(keyLength)){
	        		subsequenceTier.get(keyLength).put(entry.getKey(), entry.getValue());
	        	}
	        	else{
	        		Map<String, Integer> tierMap = new HashMap<String, Integer>();
	        		tierMap.put(entry.getKey(), entry.getValue());
	        		subsequenceTier.put(keyLength, tierMap);
	        	}
	        	
	        	// 更新最长序列的长度记录
	        	if(keyLength > maxSeqLength){
	        		maxSeqLength = keyLength;
	        	}
	        	
	        }
	        
	        newSequences.clear();
		}

		return subSequences;
	}
	
	/**
	 * 产生Closed Frequent Subsequences，满足：
	 * 		1. 是候选频繁子序列（Frequent Subsequences）的子集
	 * 		2. 满足Closed条件：与所有super-subsequences的support不同
	 * 
	 * @param subSequences	候选频繁子序列集合（Frequent Subsequences）
	 * @return	Closed Frequent Subsequences，Map<序列, Support值>
	 */
	public Map<String, Integer> closedFreSubsequences(Map<String, Integer> subSequences){
		
		Map<String, Integer> closedSubSequences = new HashMap<String, Integer>();
		
		// 每层、依次检查每一个frequent subsequence，从中挑选出closed frequent subsequence
		for(int i = this.maxSeqLength; i > 0; i--){
			
			// 最长序列都是closed的
			if(i == this.maxSeqLength){
				for(Map.Entry<String, Integer> entry: this.subsequenceTier.get(i).entrySet()){
					closedSubSequences.put(entry.getKey(), entry.getValue());
				}
			}
			
			// closed条件：
			// 不是任何frequent subsequence的子序列
			// 或
			// 是子序列 && support 与 大于父序列的support
			else{
				// 依次检查当前层每一个序列
				for(Map.Entry<String, Integer> entry: this.subsequenceTier.get(i).entrySet()){
					boolean isSubSubseq = false;
					
					// 当前序列 与 其父层（直接父序列）中每一个序列的关系：是否为子序列、support大小关系
					for(Map.Entry<String, Integer> superEntry: this.subsequenceTier.get(i + 1).entrySet()){
						
						// 是子序列
						if(superEntry.getKey().contains(entry.getKey())){
							
							isSubSubseq = true;
							
							// 大于所有父序列的support
							if(entry.getValue() > superEntry.getValue()){
								closedSubSequences.put(entry.getKey(), entry.getValue());
								
								// 虽然这一次大于，但是有可能是有小于等于的父序列的，所以需要检测完
								continue;
							}
							else{
								closedSubSequences.remove(entry.getKey());
								
								// 一票否定，无需继续检测
								break;
							}
						}
					}
					
					// 不是任何frequent subsequence的子序列
					if(!isSubSubseq){
						closedSubSequences.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		
		return closedSubSequences;
	}
	
	/**
	 * 生成关联规则，满足：
	 * 		1. 规则格式：子序列（长度>=1） -> 后续子序列（长度=1）
	 * 		2. 从Closed Frequent Subsequences中生成
	 * 		3. 每个Rule的confidence不小于minConfidence
	 * 		4. 多个Closed Frequent Subsequences产生相同的rule，取最大support作为rule的support
	 * @param subSequences			频繁子序列，用于查找support	
	 * @param closedSubSequences	Closed频繁子序列，用于生成rule
	 * @param minConfidence			Rule的最小confidence，用于过滤Rule
	 * @return	correlation rules, Map<Rule表达式，Rule对象>
	 */
	public Map<String, Rule> generateRules(Map<String, Integer> subSequences, Map<String, Integer> closedSubSequences, float minConfidence){
		
		Map<String, Rule> rules = new HashMap<String, Rule>();
		
		// 依次处理每一个closed frequent subsequence，获取rules
		for(Map.Entry<String, Integer> closedEntry: closedSubSequences.entrySet()){
			
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
					float historyConf = subSequences.get(history) * 1.0F;
				
					// 生成prediction子序列（只有一个字符）
					for(int predictionStart = historyEnd; predictionStart < closedSeq.length(); predictionStart++){
						
						String prediction = closedSeq.substring(predictionStart, predictionStart + 1);
						float newRuleConf = subSequences.get(prediction) / historyConf;
						
						// 当前规则confidence不够，跳过
						if(newRuleConf < minConfidence){
							continue;
						}
						
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
	
	public Map<String, Rule> startMining(String sequence, int windowSize, int maxGap, int minSupport, float minConfidence){

		// 对初始访问序列分段
		List<String> accessSegments = cutAccessSequence(sequence, windowSize);
		
		// 挖掘：频繁子序列
		Map<String, Integer> freSubseq = candidateFreSubsequences(accessSegments, windowSize, maxGap, minSupport);
		
		// 过滤：Closed频繁子序列
		Map<String, Integer> closedFreSubseq = closedFreSubsequences(freSubseq);
		
		// 生成：关联规则
		return generateRules(freSubseq, closedFreSubseq, minConfidence);
	}
}
