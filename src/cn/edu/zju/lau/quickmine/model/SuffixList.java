package cn.edu.zju.lau.quickmine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后缀列表，保持特性：
 * 		1. 可以快速判断一个suffix是否在列表中；
 * 		2. 列表按照support由大到小保持顺序；
 * 		3. 具有大小，列表满时，移除最近最少使用的suffix（LFU）。
 * @author yuki
 * @date 2013-11-26
 */
public class SuffixList {  
	
	private Map<String, Integer> key2Position;
	private List<Suffix> suffixList;
	private int maxSuffixSize;
	
	public SuffixList(){
		this.key2Position = new HashMap<String, Integer>();
		this.suffixList = new ArrayList<Suffix>();
		this.maxSuffixSize = 16;
	}
	
	public SuffixList(int maxSuffixSize){
		this.key2Position = new HashMap<String, Integer>();
		this.suffixList = new ArrayList<Suffix>();
		this.maxSuffixSize = maxSuffixSize;
	}
	
	@Override
	public String toString(){
		
		return suffixList.toString();
	}
	
	/**
	 * 向SuffixList中添加Suffix元素，如果：
	 * 1. 该suffix已存在：增加suffix.support
	 * 2. 该suffix不存在：添加suffix，support = 1
	 * 最后，移动suffix到相同support元素的第一个位置，更新位置映射。
	 * 
	 * @param suffixName
	 */
	public void add(String suffixName){
		
		// 初始：添加第一个suffix
		if(suffixList.size() == 0){
			suffixList.add(new Suffix(suffixName, 1));
			key2Position.put(suffixName, 0);
			return;
		}
		
		
		Integer pos = key2Position.get(suffixName);

		// suffixName第一次添加
		if(pos == null){
			int tailPos = suffixList.size() - 1;
			boolean insert = false;
			for(int i = tailPos; i >= 0; i--){
				if(1 < suffixList.get(i).getSupport()){
					insert = true;
					suffixList.add(i + 1, new Suffix(suffixName, 1));
					updateKey2Pos(suffixName, i + 1);
					break;
				}
			}
			if(!insert){
				suffixList.add(0, new Suffix(suffixName, 1));
				updateKey2Pos(suffixName, 0);
			}
			
		}
		// suffixName非第一次添加
		else{
			int newSupport = suffixList.get(pos).getSupport() + 1;
			boolean insert = false;
			for(int i = pos; i >= 0; i--){
				if(newSupport < suffixList.get(i).getSupport()){
					insert = true;
					suffixList.remove(pos);
					suffixList.add(i + 1, new Suffix(suffixName, newSupport));
					updateKey2Pos(suffixName, i + 1);
					break;
				}
			}
			if(!insert){
				suffixList.remove(pos);
				suffixList.add(0, new Suffix(suffixName, newSupport));
				updateKey2Pos(suffixName, 0);
			}
		}
		
		// 如果超出规定的List大小，移除最后一个元素
		if(suffixList.size() > maxSuffixSize){
			String key = suffixList.get(maxSuffixSize).getSuffix();
			suffixList.remove(maxSuffixSize);
			key2Position.remove(key);
		}
	}
	
	/**
	 * 更新key2position中文件名与位置的映射关系，将newPos之后的元素都依次后移一位
	 * @param suffixName
	 * @param newPos
	 */
	private void updateKey2Pos(String suffixName, Integer newPos){
		
		for(Map.Entry<String, Integer> entry: key2Position.entrySet()){
			
			if(entry.getValue() >= newPos){
				key2Position.put(entry.getKey(), entry.getValue() + 1);
			}
		}
		key2Position.put(suffixName, newPos);
	}
	
	
	/* getters and setters */

	public Map<String, Integer> getKey2Position() {
		return key2Position;
	}

	public List<Suffix> getSuffixList() {
		return suffixList;
	}

	public int getMaxSuffixSize() {
		return maxSuffixSize;
	}

	public void setMaxSuffixSize(int maxSuffixSize) {
		this.maxSuffixSize = maxSuffixSize;
	}
	
	
	/* test */
	public static void main(String[] args){
		
		SuffixList list = new SuffixList();
		
		for(int i = 0; i < 20; i++){
			list.add((i % 10) + "");
			System.out.println(list.getSuffixList());
		}
	}

}
