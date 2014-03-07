package cn.edu.zju.lau.test.simulate.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import cn.edu.zju.lau.cminer.impl.hdfs.CMinerHDFS;
import cn.edu.zju.lau.cminer.model.hdfs.HDFSRule;
import cn.edu.zju.lau.test.simulate.Simulator;

/**
 * 采用CMiner算法作为PredictCache的预测算法，模拟使用。
 * 
 * @author yuki
 * @date 2013-11-27
 */
public class CMinerSimulator extends Simulator{

	private CMinerHDFS miner;
	private String currentPrefix;
	private int currentGap = 0;
	
	/**
	 * 默认参数：
	 * 		CMiner: maxGap = 2（与QuickMine的3等价）
	 */
	protected CMinerSimulator(int fileCacheSize) {
		
		super(fileCacheSize);
		
		this.miner = new CMinerHDFS();
		this.miner.setMaxGap(2);
	}

	/**
	 * 设置CMiner数据集：文件访问日志中的文件名列表
	 * @param logs
	 */
	public void setDataSet(List<String> logs){
		this.miner.setInputSequence(logs);
	}
	
	/**
	 * 调用CMiner方法，根据文件访问日志挖掘关联规则
	 * @param logs
	 */
	public void generateRules(){
		this.miner.startMining();
	}
	
	/**
	 * 根据当前访问上文和currentFile构成的Prefix，获取后续预测文件。
	 */
	@Override
	protected List<String> getPredictFiles(String currentFile) {
		
		String nextFile = null;

		// 将当前读取的文件考虑到序列中
		if(StringUtils.isEmpty(currentPrefix)){
			currentPrefix = currentFile;
		}
		else{
			currentPrefix += ("|" + currentFile);
		}

		// 在规则中搜索序列，若找到，则返回
		boolean starts = false;
		for(Map.Entry<String, HDFSRule> entry: miner.getRules().entrySet()){
			if(entry.getKey().startsWith(currentPrefix)){
				//【只是】以当前序列开头，非相等：保证找出a->c,a|b->这种情况
				if(entry.getKey().endsWith(currentPrefix)){
					nextFile = entry.getValue().getPrediction();
				}
				else{
					starts = true;
				}
			}
		}
		
		// 规则中未找到该序列，有四种可能：
		if(currentPrefix.equals(currentFile)){
			// 1. 序列仅包含一个文件，且没有匹配的：跳过，从下一个文件重新开始
			if(!starts){
				currentPrefix = null;
				currentGap = 0;
			}
			// 2. 有匹配的序列前缀，但是并未完整形成：以当前序列为基础，继续生成 
			else{
				currentGap = 0;
			}
		}
		else{
			// 1. 没有匹配到规则序列，且超过maxGap：直接以当前文件作为新序列开头，重新生成
			if((!starts) && currentGap > miner.getMaxGap()){
				currentPrefix = currentFile;
				currentGap = 0;
			}
			// 2. 有匹配的序列前缀，但是并未完整形成：以当前序列为基础，继续生成
			else if(starts){
				currentGap = 0;
			}
			// 3. 没有匹配的序列前缀，且不是序列开头，Gap也没有超过：让其在gap范围内继续扩展一层
			else{
				currentGap++;
			}
		}
		
		List<String> predictSuffixes = new ArrayList<String>();
		if(!StringUtils.isEmpty(nextFile)){
			predictSuffixes.add(nextFile);
		}

		return predictSuffixes;
	}

	/**
	 * Test Start
	 * @param args
	 */
	public static void main(String[] args){
		
		for(int fileCacheSize = 1; fileCacheSize <= 50; fileCacheSize++){
				
			CMinerSimulator simulator = new CMinerSimulator(fileCacheSize);
			
			// 获取数据集
			List<String> logs = simulator.getDataSet("D://audit-interleaving.log", "/user/root/input/sogou/query-log-");
			
			// 生成关联规则
			simulator.setDataSet(logs);
			simulator.generateRules();
			
			// 模拟读取数据，利用关联规则提高Cache命中率
			int hitCount = 0;
			int prefetchCount = 0;
			long totalTime = 0;
			for(int i = 0; i< logs.size(); i++){
				
				String currentFile = logs.get(i);
				
				long start = System.nanoTime();
				String targetFile = simulator.getFileFromCache(currentFile);
				// Miss
				if(targetFile == null){
					// time of getting file from disk
//					try {
//						Thread.sleep(1);
//					} 
//					catch (InterruptedException e) {
//						e.printStackTrace();
//					}
					
					// read miss causes prediction
					for(String file: simulator.getPredictFiles(currentFile)){
						simulator.putFileIntoCache(file, file);
						prefetchCount++;
					}
				}
				// Hit
				else{
					hitCount++;
				}
				long end = System.nanoTime();
				
				totalTime += (end - start);
			}
			
			// 输出命中率
			// System.out.println("CMiner Hit Ratio: " + (hitCount * 1.0 / logs.size()));
			System.out.println(hitCount * 1.0 / logs.size());
			// System.out.println(prefetchCount);
			// System.out.println(totalTime * 1.0 / logs.size() / 1000000);
		}
	}
}
