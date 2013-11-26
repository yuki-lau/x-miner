package cn.edu.zju.lau.utils;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用LinkedHashMap实现的LRU Cache，通过重写removeEldestEntry方法指定replacement的时机。
 * @author yuki
 * @date 2013-11-26
 * 
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V>{

	private static final long serialVersionUID = -9213983456112059461L;
	private static float DEFAULT_LOAD_FACTOR = 0.75F;
	private int maxCapacity;
	private final Lock lock = new ReentrantLock();
	
	public LRUCache(int maxCapacity){
		super(maxCapacity, DEFAULT_LOAD_FACTOR, true);
		this.maxCapacity = maxCapacity;
	}
	
	public LRUCache(int maxCapacity, int loadFactor){
		super(maxCapacity, loadFactor, true);
		this.maxCapacity = maxCapacity;
		LRUCache.DEFAULT_LOAD_FACTOR = loadFactor;
	}

	@Override
	protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
		
		// 在Cache容量超过capacity时移除最老的entry
		return size() > maxCapacity;
	}

	@Override
	public V get(Object key) {
		
		// 保证线程安全，由于每次LinkedHashMap的get都会发生写操作，因此get/put共享锁
		try{
			lock.lock();
			return super.get(key);
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public V put(K key, V value) {
		
		// 保证线程安全
		try{
			lock.lock();
			return super.put(key, value);
		}
		finally{
			lock.unlock();
		}
	}
	
	
	
}

