package com.apm70.fileq.util;

import java.util.ArrayList;
import java.util.List;

public class DestroyBeanShutdownHook {

    private static final List<Destroyable> destroyableBeans = new ArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }));
    }

    private DestroyBeanShutdownHook() {
    }

    public static void shutdown() {
        destroyableBeans.stream().forEach(d -> {
            try {
                d.destroy();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void addLast(final Destroyable bean) {
        destroyableBeans.add(bean);
    }

    public static void addFirst(final Destroyable bean) {
        destroyableBeans.add(0, bean);
    }
}
