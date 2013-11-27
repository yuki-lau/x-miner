package cn.edu.zju.lau.test.simulate.impl;

import java.util.List;

import cn.edu.zju.lau.test.simulate.Simulator;

/**
 * 不采用预测算法，直接使用LRUCache，模拟使用。
 * 
 * @author yuki
 * @date 2013-11-27
 */
public class NonPredictSimulator extends Simulator{

	protected NonPredictSimulator(int fileCacheSize) {
		
		super(fileCacheSize);
	}

	/**
	 * 无预测，空方法。
	 */
	@Override
	protected List<String> getPredictFiles(String currentFile) {
		
		return null;
	}

	/**
	 * Test Start
	 * @param args
	 */
	public static void main(String[] args){
		
		int fileCacheSize = 1024;
		NonPredictSimulator simulator = new NonPredictSimulator(fileCacheSize);
		
		// 获取数据集
		List<String> logs = simulator.getDataSet("D://audit.log", "/user/root/input/sogou/query-log-");
		
		// 生成关联规则：无预测，不需要预先生成关联规则
		
		// 模拟读取数据，只是用FileCache的LRU替换规则，不用预测
		int hitCount = 0;
		for(int i = 0; i< logs.size(); i++){
			// Hit
			if(simulator.getFileFromCache(logs.get(i)) != null){
				hitCount++;
			}
		}
		
		// 输出命中率
		System.out.println("Non-Predict Hit Ratio: " + (hitCount * 1.0 / logs.size()));
	}
}
