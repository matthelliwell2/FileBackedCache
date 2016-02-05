package org.matthelliwell.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableSet;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


/**
 * This is an implementation of a map that uses a LRU maps to store objects in memory and then a set of serialised
 * files that are used to store the objects when the LRU cache is full
 */
public class FileBackedCache<K, V extends Serializable> implements Map<K, V> {
    private static final int DEFAULT_MEMORY_CACHE_CAPACITY = Integer.MAX_VALUE;

    private int memoryCacheCapacity;
    private Optional<BiConsumer<K, V>> deserialisedCallback = Optional.empty();
    private Map<K, Path> fileCache = new HashMap<>();
    private Optional<Path> tempDir = Optional.empty();
    private LinkedHashMap<K, V> memoryCache = new LinkedHashMap<K, V>() {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            if (size() > memoryCacheCapacity) {
                saveEntryToFile(eldest);
                return true;
            } else {
                return false;
            }
        }
    };

    /**
     * This will in effect never serialise anything to cache and will store everything in memory. It is useful for
     * tuning to see how many items you can store in memory
     */
    public FileBackedCache() {
        this(DEFAULT_MEMORY_CACHE_CAPACITY, null);
    }

    public FileBackedCache(final int memoryCacheCapacity) {
        this(memoryCacheCapacity, null);
    }

    public FileBackedCache(final BiConsumer<K, V> deserialisedCallback) {
        this(DEFAULT_MEMORY_CACHE_CAPACITY, deserialisedCallback);
    }

    public FileBackedCache(final int memoryCacheCapacity, final BiConsumer<K, V> deserialisedCallback) {
        this.memoryCacheCapacity = memoryCacheCapacity;

        this.deserialisedCallback = Optional.ofNullable(deserialisedCallback);
    }


    @Override
    public int size() {
        return fileCache.size() + memoryCache.size();
    }

    @Override
    public boolean isEmpty() {
        return memoryCache.isEmpty() && fileCache.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return memoryCache.containsKey(key) || fileCache.containsKey(key);
    }

    /**
     * We'd have to load each file and check for the value to implement this. If we could do this, we wouldn't need
     * this class
     */
    @Override
    public boolean containsValue(final Object value) {
        throw new NotImplementedException();
    }

    @Override
    public V get(final Object key) {
        final V memoryCachedObject = memoryCache.get(key);
        if (memoryCachedObject == null) {
            // Check the file cache
            final Path path = fileCache.get(key);
            if (path != null) {
                final V fileCachedObject = readEntryFile(path);

                // We've read it from file so add it to the memory cache, possibly knocking out something else
                memoryCache.put((K)key, fileCachedObject);
                removeFromFileCache(key);

                deserialisedCallback.ifPresent(cb -> cb.accept((K)key, fileCachedObject));

                return fileCachedObject;
            }
        }

        return memoryCachedObject;
    }

    @Override
    public V put(final K key, final V value) {
        return memoryCache.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        if (memoryCache.containsKey(key)) {
            return memoryCache.remove(key);
        } else if ( fileCache.containsKey(key)) {
            return removeFromFileCache(key);
        }

        return null;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    /** Call this to remove all temporary file and directory */
    @Override
    public void clear() {
        memoryCache.clear();
        fileCache.entrySet().forEach((e) -> deleteEntryFile(e.getValue()));
        fileCache.clear();

        // After clearing everything, delete the temp directory so it doesn't hang around
        tempDir.ifPresent((path) -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        tempDir = Optional.empty();

    }

    @Override
    public Set<K> keySet() {
        return ImmutableSet.<K>builder()
                .addAll(memoryCache.keySet())
                .addAll(fileCache.keySet())
                .build();
    }

    /**
     * We'd have to load each file and check for the value to implement this. If we could do this, we wouldn't need
     * this class
     */
    @Override
    public Collection<V> values() {
        throw new NotImplementedException();
    }

    /**
     * We'd have to load each file and check for the value to implement this. If we could do this, we wouldn't need
     * this class
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new NotImplementedException();
    }

    private void saveEntryToFile(final Map.Entry<K, V> entry) {
        createTempDirIfDoesntExist();
        final Path path = writeEntryFile(entry);
        fileCache.put(entry.getKey(), path);
    }

    private V removeFromFileCache(final Object key) {
        final Path entryFile = fileCache.get(key);
        final V cachedObject = readEntryFile(entryFile);
        fileCache.remove(key);
        deleteEntryFile(entryFile);

        return cachedObject;
    }

    private void createTempDirIfDoesntExist() {
        try {
            if (!tempDir.isPresent()) {
                tempDir = Optional.of(Files.createTempDirectory("filebackedcache"));
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path writeEntryFile(final Entry entry) {
        try {
            final Path entryPath = Files.createTempFile(tempDir.get(), entry.getKey().toString() + "-", ".ser");
            try (final OutputStream os = Files.newOutputStream(entryPath)) {
                final ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(entry.getValue());
            }

            return entryPath;

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private V readEntryFile(final Path path) {
        try {
            try (final InputStream is = Files.newInputStream(path)) {
                final ObjectInputStream ois = new ObjectInputStream(is);

                return (V)ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteEntryFile(final Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



