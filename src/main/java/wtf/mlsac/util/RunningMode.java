package wtf.mlsac.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class RunningMode {
    
    private static final double THRESHOLD = 1e-3;
    
    private final Queue<Double> addList;
    private final Map<Double, Integer> popularityMap;
    private final int maxSize;
    
    public RunningMode(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("There's no mode to a size 0 or negative list!");
        }
        this.addList = new ArrayDeque<>(maxSize);
        this.popularityMap = new HashMap<>();
        this.maxSize = maxSize;
    }
    
    public int size() {
        return addList.size();
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public void add(double value) {
        pop();
        
        for (Map.Entry<Double, Integer> entry : popularityMap.entrySet()) {
            if (Math.abs(entry.getKey() - value) < THRESHOLD) {
                entry.setValue(entry.getValue() + 1);
                addList.add(entry.getKey());
                return;
            }
        }
        
        popularityMap.put(value, 1);
        addList.add(value);
    }
    
    private void pop() {
        if (addList.size() >= maxSize) {
            Double type = addList.poll();
            if (type != null) {
                Integer popularity = popularityMap.get(type);
                if (popularity != null) {
                    if (popularity == 1) {
                        popularityMap.remove(type);
                    } else {
                        popularityMap.put(type, popularity - 1);
                    }
                }
            }
        }
    }
    
    public Pair<Double, Integer> getMode() {
        int max = 0;
        Double mostPopular = null;
        
        for (Map.Entry<Double, Integer> entry : popularityMap.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                mostPopular = entry.getKey();
            }
        }
        
        return new Pair<>(mostPopular, max);
    }
    
    public void clear() {
        addList.clear();
        popularityMap.clear();
    }
}
