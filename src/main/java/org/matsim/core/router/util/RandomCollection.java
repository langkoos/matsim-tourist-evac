package org.matsim.core.router.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RandomCollection<E> {
    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private final NavigableMap<Double, E> rawMap = new TreeMap<Double, E>();
    private final Map<E, Double> weightLookupMap = new HashMap<>();
    private final NavigableMap<Double, E> invertMap = new TreeMap<Double, E>();
    private final Random random;
    private double total = 0;
    private double invTotal = 0;

    public RandomCollection() {
        this(new Random());
    }

    public RandomCollection(Random random) {
        this.random = random;
    }

    public RandomCollection<E> add(double weight, E result) {
        if (weight <= 0)
            return this;
        total += weight;
        rawMap.put(weight, result);
        weightLookupMap.put(result, weight);
        createInverseMap(total);
        return this;
    }

    public E invertSelect() {

        double value = invTotal * random.nextDouble();

        return invertMap.higherEntry(value).getValue();
    }

    public E select() {
        double value = total * random.nextDouble();

        return map.higherEntry(value).getValue();
    }

    public boolean removeAsset(String asset) {
        AtomicReference<Double> target = new AtomicReference<>(null);
        this.rawMap.entrySet().forEach(entry -> {
            if (entry.getValue().equals(asset))
                target.set(entry.getKey());
        });
        if (target.get() == null)
            return false;
        E remove = rawMap.remove(target.get());
        weightLookupMap.remove(remove);
        total -= target.get();
        createInverseMap(total);
        return true;
    }

    /**
     * both inverted and non-inverted distributions will be trimmed
     *
     * @param fraction
     *         the size of the set will be reduced to this value
     */
    public void trim(double fraction) {
        int finalSetSize = (int) (fraction * (double) rawMap.size());
        total = new ArrayList<Double>(this.map.keySet()).subList(0, finalSetSize).get(finalSetSize - 1);
        AtomicReference<Double> total = new AtomicReference<>(0d);
        Set<Double> rawBads = new HashSet<>();
        rawMap.entrySet().forEach(entry -> {
            if (total.get() < this.total) {
                total.accumulateAndGet(entry.getKey(), Double::sum);
            } else
                rawBads.add(entry.getKey());
        });
        rawBads.forEach(key -> {
            E remove = rawMap.remove(key);
            weightLookupMap.remove(remove);
        });
        createInverseMap(total.get());
    }
    public double getWeight(E key){
        return weightLookupMap.get(key);
    }

    private void createInverseMap(double total) {
        invertMap.clear();
        map.clear();
        this.total = total;
        invTotal = rawMap.keySet().stream().map(key -> total / key).collect(Collectors.toList()).stream().reduce(Double::sum).get();
        AtomicReference<Double> runningTotal = new AtomicReference(0d);
        AtomicReference<Double> runningInvTotal = new AtomicReference(0d);
        rawMap.entrySet().forEach(entry -> map.put(runningTotal.accumulateAndGet(entry.getKey(), Double::sum), entry.getValue()));
        rawMap.entrySet().forEach(entry -> invertMap.put(runningInvTotal.accumulateAndGet(total / entry.getKey(), Double::sum), entry.getValue()));
    }
}