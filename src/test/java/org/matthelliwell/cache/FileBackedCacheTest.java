package org.matthelliwell.cache;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FileBackedCacheTest {

    private int count;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private FileBackedCache<Integer, String> testSubject;

    @After
    public void tearDown() {
        testSubject.clear();
    }

    @Test
    public void shouldGetObjectFromMemory() {
        // given
        testSubject = new FileBackedCache<>();
        testSubject.put(3, "three");

        // when
        final String result = testSubject.get(3);

        // then
        assertThat(result, is("three"));
    }

    @Test
    public void shouldGetObjectFromFile() {
        // given
        testSubject = new FileBackedCache<>(3);
        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // when
        final String result1 = testSubject.get(1);
        final String result2 = testSubject.get(2);
        final String result3 = testSubject.get(3);
        final String result4 = testSubject.get(4);
        final String result5 = testSubject.get(5);

        // then
        assertThat(result1, is("one"));
        assertThat(result2, is("two"));
        assertThat(result3, is("three"));
        assertThat(result4, is("four"));
        assertThat(result5, is("five"));
    }

    @Test
    public void shouldGetSizeAfterAddingObjects() {
        // given
        testSubject = new FileBackedCache<>(3);
        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // then
        assertThat(testSubject.size(), is(5));
        assertThat(testSubject.isEmpty(), is(false));
    }

    @Test
    public void shouldReturnEmptyForNewCache() {
        // given
        testSubject = new FileBackedCache<>();

        // then
        assertThat(testSubject.size(), is(0));
        assertThat(testSubject.isEmpty(), is(true));
    }

    @Test
    public void shouldContainKey() {
        // given
        testSubject = new FileBackedCache<>(3);
        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // then
        assertThat(testSubject.containsKey(1), is(true));
        assertThat(testSubject.containsKey(2), is(true));
        assertThat(testSubject.containsKey(3), is(true));
        assertThat(testSubject.containsKey(4), is(true));
        assertThat(testSubject.containsKey(5), is(true));
        assertThat(testSubject.containsKey(6), is(false));
    }

    @Test
    public void shouldCallDeserialisedCallback() {
        // given
        count = 0;
        testSubject = new FileBackedCache<>(3, this::callback);

        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // when
        testSubject.get(5);
        testSubject.get(4);
        testSubject.get(3);
        testSubject.get(2);
        testSubject.get(1);

        // then
        assertThat(count, is(2));
    }

    public void callback(int key, String value) {
        ++count;
    }

    @Test
    public void shouldThrowExceptionForContainsValue() {
        // given
        thrown.expect(NotImplementedException.class);

        testSubject = new FileBackedCache<>();

        // when
        testSubject.containsValue("one");
    }

    @Test
    public void shouldThrowExceptionForValues() {
        // given
        thrown.expect(NotImplementedException.class);

        testSubject = new FileBackedCache<>();

        // when
        testSubject.values();
    }

    @Test
    public void shouldThrowExceptionForEntrySet() {
        // given
        thrown.expect(NotImplementedException.class);

        testSubject = new FileBackedCache<>();

        // when
        testSubject.entrySet();
    }

    @Test
    public void shouldReturnKeySet() {
        // given
        testSubject = new FileBackedCache<>(3);
        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // when
        final Set<Integer> result = testSubject.keySet();

        // then
        assertThat(result.size(), is(5));
    }

    @Test
    public void shouldRemoveEntries() {
        // given
        testSubject = new FileBackedCache<>(3);
        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // when
        final String result1 = testSubject.remove(1);
        final String result2 = testSubject.remove(5);

        // then
        assertThat(result1, is("one"));
        assertThat(result2, is("five"));
        assertThat(testSubject.size(), is(3));
    }

    @Test
    public void shouldAddAll() {
        // given
        final Map<Integer, String> input = ImmutableMap.of(1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
        testSubject = new FileBackedCache<>(3);

        // when
        testSubject.putAll(input);

        // then
        assertThat(testSubject.size(), is(5));
    }

    @Test
    public void shouldClear() {
        // given
        testSubject = new FileBackedCache<>(3);
        testSubject.put(1, "one");
        testSubject.put(2, "two");
        testSubject.put(3, "three");
        testSubject.put(4, "four");
        testSubject.put(5, "five");

        // when
        testSubject.clear();

        // then
        assertThat(testSubject.size(), is(0));
    }

}