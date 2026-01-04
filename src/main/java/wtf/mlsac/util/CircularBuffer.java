/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file contains code derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 * All derived code is licensed under GPL-3.0.
 */

package wtf.mlsac.util;

import java.util.ArrayList;
import java.util.List;

public class CircularBuffer<T> {
    
    private final Object[] buffer;
    private final int capacity;
    private int head;
    private int size;
    
    public CircularBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }
    
    public void add(T item) {
        buffer[head] = item;
        head = (head + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }
    
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        int actualIndex = (head - size + index + capacity) % capacity;
        return (T) buffer[actualIndex];
    }
    
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(get(i));
        }
        return result;
    }
    
    public void clear() {
        for (int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
        head = 0;
        size = 0;
    }
    
    public int size() {
        return size;
    }
    
    public int capacity() {
        return capacity;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public boolean isFull() {
        return size == capacity;
    }
}
