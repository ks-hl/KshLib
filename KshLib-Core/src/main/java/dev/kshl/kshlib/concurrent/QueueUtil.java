package dev.kshl.kshlib.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

public class QueueUtil {
    public static <E> void pollAll(Queue<E> queue, Consumer<E> consumer) {
        E e;
        while ((e = queue.poll()) != null) {
            consumer.accept(e);
        }
    }

    public static <E> List<E> pollAll(Queue<E> queue) {
        List<E> list = new ArrayList<>();
        pollAll(queue, list::add);
        return list;
    }
}
