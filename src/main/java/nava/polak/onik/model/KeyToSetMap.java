package nava.polak.onik.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeyToSetMap<K,V> {
    private Map<K,Set<V>> map = new HashMap<>();


    public V put(K key, V value) {
        Set<V> keySet =  map.get(key);
        if(keySet == null)
            keySet = new HashSet<>();
        keySet.add(value);
        map.put(key,keySet);
        return value;
    }

    public Set<V> get(K key){
        return map.get(key);
    }
}
