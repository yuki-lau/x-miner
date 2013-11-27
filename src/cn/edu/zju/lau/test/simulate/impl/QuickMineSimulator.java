package cn.edu.zju.lau.test.simulate.impl;

import java.util.ArrayList;
import java.util.List;

import cn.edu.zju.lau.quickmine.QuickMine;
import cn.edu.zju.lau.quickmine.model.Suffix;
import cn.edu.zju.lau.test.simulate.Simulator;

/**
 * 采用QuickMine算法作为PredictCache的预测算法，模拟使用。
 * 
 * @author yuki
 * @date 2013-11-27
 */
public class QuickMineSimulator extends Simulator{

	private QuickMine miner;
	private int prefetchNum;
	
	/**
	 * 默认参数：
	 * 		Prefetch Number: 3
	 * 		QuickMine: maxPrefixNum = 1024, maxSuffixNum = 8, maxGap = 3, prefetchNum = 3
	 */
	protected QuickMineSimulator(int fileCacheSize) {
		
		super(fileCacheSize);
		
		this.prefetchNum = 3;
		this.miner = new QuickMine(1024, 8, 3, 3);
	}

	/**
	 * 根据当前访问的前两次访问构成的序列，获取后续预测文件。
	 * 参数currentFile在这儿没用。
	 */
	@Override
	protected List<String> getPredictFiles(String currentFile) {
		
		List<String> predictSuffixes = new ArrayList<String>();
		List<String> history = miner.getCurrentLogs();
		int historySize = history.size();

		if(historySize >= 2 ){
			String prefix = history.get(historySize - 2) + "|" + history.get(historySize - 1);
			List<Suffix> candidateSuffix = miner.getPredictSuffix(prefix);
			
			int count = 0;
			for(int i = 0; i < candidateSuffix.size() && count < this.prefetchNum; i++){
				String suffix = candidateSuffix.get(i).getSuffix();
				if(FILE_CACHE.containsKey(suffix)){
					continue;
				}
				else{
					predictSuffixes.add(suffix);
					count++;
				}
			}
		}
		
		return predictSuffixes;
	}

	/**
	 * 根据当前访问文件，增量的产生新规则
	 * @param currentFile
	 */
	public void generateNewRule(String currentFile){
		miner.miningByStep(currentFile);
	}
	
	/**
	 * Test Start
	 * @param args
	 */
	public static void main(String[] args){
		
		int fileCacheSize = 1024;
		QuickMineSimulator simulator = new QuickMineSimulator(fileCacheSize);
		
		// 获取数据集
		List<String> logs = simulator.getDataSet("D://audit.log", "/user/root/input/sogou/query-log-");
		
		// 生成关联规则：QuickMine 不需要预先生成关联规则
		
		// 模拟读取数据，利用关联规则提高Cache命中率
		int hitCount = 0;
		for(int i = 0; i< logs.size(); i++){
			
			String currentFile = logs.get(i);
			
			// Miss
			if(simulator.getFileFromCache(currentFile) == null){
				for(String file: simulator.getPredictFiles(currentFile)){
					simulator.putFileIntoCache(file, file);
				}
			}
			// Hit
			else{
				hitCount++;
			}
			
			// generate new rules incrementally
			simulator.generateNewRule(currentFile);
		}
		
		// 输出命中率
		System.out.println("QuickMine Hit Ratio: " + (hitCount * 1.0 / logs.size()));
	}
}
