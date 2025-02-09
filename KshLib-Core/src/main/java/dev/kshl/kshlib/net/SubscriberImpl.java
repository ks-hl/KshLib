package dev.kshl.kshlib.net;

import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public class SubscriberImpl<T> implements Flow.Subscriber<T> {
    private final SubscriberImpl<?> parent;

    public SubscriberImpl() {
        this(null);
    }

    public SubscriberImpl(SubscriberImpl<?> parent) {
        this.parent = parent;
    }

    private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (subscriptionRef.compareAndSet(null, subscription)) {
            subscription.request(Long.MAX_VALUE);
        } else {
            // If a subscription is already present, cancel the new one
            subscription.cancel();
        }
    }

    @Override
    public void onNext(T s) {
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onComplete() {
    }

    public void close() {
        Optional.ofNullable(subscriptionRef.get()).ifPresent(Flow.Subscription::cancel);
    }
}
