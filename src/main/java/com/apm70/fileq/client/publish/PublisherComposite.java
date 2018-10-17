package com.apm70.fileq.client.publish;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PublisherComposite implements Publisher {

    private final Set<Publisher> publishers = new HashSet<>();
    private final ConcurrentHashMap<Class<? extends PublishContext>, Publisher> cache = new ConcurrentHashMap<>();

    public void addPublisher(final Publisher publisher) {
        this.publishers.add(publisher);
    }

    @Override
    public void registerPublishStateListener(final PublishStateListener listener) {
        this.publishers.stream().forEach(publisher -> publisher.registerPublishStateListener(listener));
    }

    @Override
    public void registerCancelRepublish(WaitCancelRepublish cancleRepublish) {
        this.publishers.stream().forEach(publisher -> publisher.registerCancelRepublish(cancleRepublish));
    }

    @Override
    public void publishAsync(final PublishContext context) {
        Publisher publisher = this.cache.get(context.getClass());
        if (publisher == null) {
            publisher = this.publishers.stream()
                    .filter(p -> p.matches(context))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("没有合适的发布器"));
            this.cache.put(context.getClass(), publisher);
        }
        publisher.publishAsync(context);
    }

    @Override
    public boolean matches(final PublishContext context) {
        return true;
    }

}
