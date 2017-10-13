package com.vector.rxjava2learndemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SimpleActivity extends AppCompatActivity {

    private static final String TAG = SimpleActivity.class.getSimpleName();
    ListView _logsList;
    private ArrayList<String> _logs;
    private LogAdapter _adapter;
    private Subscription sSubscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);

        _logsList = (ListView) findViewById(R.id.lv_log);

        setupLogger();
    }

    public void simple(View view) {
        Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> observableEmitter) throws Exception {
                observableEmitter.onNext("1");
                observableEmitter.onNext("2");
                observableEmitter.onNext("3");
                observableEmitter.onNext("4");
                observableEmitter.onComplete();

            }
        }).subscribe(new Observer<String>() {

            private Disposable mDisposable;

            @Override
            public void onSubscribe(Disposable disposable) {
                d("onSubscribe");
                mDisposable = disposable;
            }

            @Override
            public void onNext(String s) {
                d("onNext=" + s);
            }

            @Override
            public void onError(Throwable throwable) {
                d("onError");

            }

            @Override
            public void onComplete() {
                d("onComplete");

            }
        });
    }

    public void mapSample(View view) {
        Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> observableEmitter) throws Exception {
                d(TAG, "observableEmitter's thread=" + Thread.currentThread().getId() + ",string=true");
                observableEmitter.onNext("true");
                d(TAG, "observableEmitter's thread=" + Thread.currentThread().getId() + ",string=false");
                observableEmitter.onNext("false");
                d(TAG, "observableEmitter's thread=" + Thread.currentThread().getId() + ",onComplete");
                observableEmitter.onComplete();
            }
            //1.指定了subscribe方法执行的线程，并进行第一次下游线程的切换，将其切换到新的子线程。
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .map(s ->
                {
                    d(TAG, "apply's thread=" + Thread.currentThread().getId() + ",s=" + s);
                    return "true".equals(s);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {

                    @Override
                    public void onSubscribe(Disposable disposable) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        d(TAG, "Observer's thread=" + Thread.currentThread().getId() + ",boolean=" + aBoolean);
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {
                        d(TAG, "Observer's thread=" + Thread.currentThread().getId() + ",onComplete");
                    }
                });


    }

    /**
     * FlatMap不保证不同水管之间事件的顺序，如果需要保证顺序，则需要使用contactMap
     *
     * @param view
     */
    public void flatMapSample(View view) {
        Observable<Integer> sourceObservable = Observable.create(new ObservableOnSubscribe<Integer>() {

            @Override
            public void subscribe(ObservableEmitter<Integer> observableEmitter) throws Exception {
                d(TAG, "flatMapOrderSample emit 1");
                observableEmitter.onNext(1);
                d(TAG, "flatMapOrderSample emit 2");
                observableEmitter.onNext(2);
                d(TAG, "flatMapOrderSample emit 3");
                observableEmitter.onNext(3);
            }
        });
        Observable<String> flatObservable = sourceObservable.flatMap(new Function<Integer, ObservableSource<String>>() {

            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
                d(TAG, "flatMapOrderSample apply=" + integer);
                long delay = (3 - integer) * 100;
                return Observable.fromArray("a value of " + integer, "b value of " + integer).delay(delay, TimeUnit.MILLISECONDS);
            }
        });
        flatObservable.subscribe(s -> d(TAG, s));

    }

    public void contactMapOrderSample(View view) {
        Observable<Integer> sourceObservable = Observable.create(new ObservableOnSubscribe<Integer>() {

            @Override
            public void subscribe(ObservableEmitter<Integer> observableEmitter) throws Exception {
                d(TAG, "contactMapOrderSample emit 1");
                observableEmitter.onNext(1);
                d(TAG, "contactMapOrderSample emit 1");
                observableEmitter.onNext(2);
                d(TAG, "contactMapOrderSample emit 1");
                observableEmitter.onNext(3);
            }
        });
        Observable<String> flatObservable = sourceObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .concatMap(new Function<Integer, ObservableSource<String>>() {

                    @Override
                    public ObservableSource<String> apply(Integer integer) throws Exception {
                        d(TAG, "contactMapOrderSample apply=" + integer);
                        long delay = (3 - integer) * 100;
                        return Observable.fromArray("a value of " + integer, "b value of " + integer).delay(delay, TimeUnit.MILLISECONDS);
                    }
                });
        flatObservable.subscribe(s -> d(TAG, s));
    }

    /**
     * Zip通过一个函数从多个Observable每次各取出一个事件，合并成一个新的事件发送给下游。
     * 组合的顺序是严格按照事件发送的顺序来的。
     * 最终下游收到的事件数量和上游中发送事件最少的那一根水管的事件数量相同。
     *
     * @param view
     */
    public void zipSample(View view) {
        Observable<Integer> sourceObservable = Observable.create(new ObservableOnSubscribe<Integer>() {

            @Override
            public void subscribe(ObservableEmitter<Integer> observableEmitter) throws Exception {
                d(TAG, "sourceObservable emit 1");
                observableEmitter.onNext(1);
                Thread.sleep(1000);
                d(TAG, "sourceObservable emit 2");
                observableEmitter.onNext(2);
                d(TAG, "sourceObservable emit 3");
                observableEmitter.onNext(3);
                d(TAG, "sourceObservable emit 4");
                observableEmitter.onNext(4);
            }
        }).subscribeOn(Schedulers.newThread());
        Observable<Integer> otherObservable = Observable.create(new ObservableOnSubscribe<Integer>() {

            @Override
            public void subscribe(ObservableEmitter<Integer> observableEmitter) throws Exception {
                d(TAG, "otherObservable emit 1");
                observableEmitter.onNext(1);
                d(TAG, "otherObservable emit 2");
                observableEmitter.onNext(2);
                d(TAG, "otherObservable emit 3");
                observableEmitter.onNext(3);
            }
        }).subscribeOn(Schedulers.newThread());
        Observable.zip(sourceObservable, otherObservable, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer integer, Integer integer2) throws Exception {
                return integer + integer2;
            }

        })
//                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onSubscribe(Disposable disposable) {
                        d(TAG, "resultObservable onSubscribe");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        d(TAG, "resultObservable onNext=" + integer);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        d(TAG, "resultObservable onError");
                    }

                    @Override
                    public void onComplete() {
                        d(TAG, "resultObservable onComplete");

                    }
                });
    }

    /**
     * Observable 不支持背压，缓存过多的数据容易OOM
     *
     * @param view
     */
    public void oomSample(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {

            @Override
            public void subscribe(ObservableEmitter<Integer> observableEmitter) throws Exception {
                for (int i = 0; i < 1000; i++) {
                    d(TAG, "observableEmitter=" + i);
                    observableEmitter.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe(integer -> {
                    Thread.sleep(2000);
                    d(TAG, "accept=" + integer);
                });
    }

    /**
     * 当上游和下游位于同一个线程时，如果上游发送的事件超过了下游声明的request(n)的值，那么会抛出MissingBackpressureException异常
     *
     * @param view
     */
    public void flowSample(View view) {
        Flowable<Integer> sourceFlow = Flowable.create(new FlowableOnSubscribe<Integer>() {

            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
                emitter.onComplete();
            }

        }, BackpressureStrategy.ERROR);

        sourceFlow.subscribe(new Subscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                d(TAG, "onSubscribe");
                subscription.request(2);
            }

            @Override
            public void onNext(Integer integer) {
                d(TAG, "onNext=" + integer);
            }

            @Override
            public void onError(Throwable throwable) {
                d(TAG, "onError");
            }

            @Override
            public void onComplete() {
                d(TAG, "onComplete");
            }
        });
    }

    /**
     * 当上游和下游位于不同线程时，如果上游发送的事件超过了下游的声明，事件会被放在水缸当中，这个水缸默认的大小是128，
     * 只有当下游调用request时，才从水缸中取出事件发送给下游，如果水缸中事件的个数超过了128，那么也会抛出MissingBackpressureException异常
     *
     * @param view
     */
    public void flowSample1(View view) {
        Flowable<Integer> sourceFlow = Flowable.create(new FlowableOnSubscribe<Integer>() {

            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
                emitter.onComplete();
            }

        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io());

        sourceFlow.observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        d(TAG, "onSubscribe");
                        sSubscription = subscription;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        d(TAG, "onNext=" + integer);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        d(TAG, "onError");
                    }

                    @Override
                    public void onComplete() {
                        d(TAG, "onComplete");
                    }
                });
    }

    public void clickSubscription(View view) {
        if (sSubscription != null) {
            sSubscription.request(10);
        }
    }

    /**
     * 使用BUFFER策略时，相当于在上游放置了一个容量无限大的水缸，所有下游暂时无法处理的消息都放在水缸当中，这里不再像ERROR策略一样，区分上游和下游是否位于同一线程。
     * 因此，如果下游一直没有处理消息，那么将会导致内存一直增长，从而引起OOM。
     *
     * @param view
     */
    public void flowBufferSample(View view) {
        Flowable<Integer> sourceFlow = Flowable.create(new FlowableOnSubscribe<Integer>() {

            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; i < 10000; i++) {
                    emitter.onNext(i);
                }
                emitter.onComplete();
            }

        }, BackpressureStrategy.BUFFER).subscribeOn(Schedulers.io());

        sourceFlow.observeOn(Schedulers.newThread()).subscribe(new Subscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                d(TAG, "onSubscribe");
                sSubscription = subscription;
            }

            @Override
            public void onNext(Integer integer) {
                d(TAG, "onNext=" + integer);
            }

            @Override
            public void onError(Throwable throwable) {
                d(TAG, "onError");
            }

            @Override
            public void onComplete() {
                d(TAG, "onComplete");
            }
        });
    }

    /**
     * 使用DROP策略时，会把水缸无法存放的事件丢弃掉，这里同样不会受到下游和下游是否处于同一个线程的限制
     *
     * @param view
     */
    public void flowDropSample(View view) {
        Flowable<Integer> sourceFlow = Flowable.create(new FlowableOnSubscribe<Integer>() {

            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; i < 130; i++) {
                    emitter.onNext(i);
                }
            }

        }, BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io());

        sourceFlow.observeOn(Schedulers.io())
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        d(TAG, "onSubscribe");
                        sSubscription = subscription;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        d(TAG, "onNext=" + integer);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        d(TAG, "onError");
                    }

                    @Override
                    public void onComplete() {
                        d(TAG, "onComplete");
                    }
                });
    }

    /**
     * 和DROP类似，当水缸无法容纳下消息时，会将它丢弃，但是除此之外，上游还会缓存最新的一条消息
     *
     * @param view
     */
    public void flowLatestSample(View view) {
        Flowable<Integer> sourceFlow = Flowable.create(new FlowableOnSubscribe<Integer>() {

            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; i < 130; i++) {
                    emitter.onNext(i);
                }
            }

        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io());

        sourceFlow.observeOn(Schedulers.io())
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        d(TAG, "onSubscribe");
                        sSubscription = subscription;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        d(TAG, "onNext=" + integer);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        d(TAG, "onError");
                    }

                    @Override
                    public void onComplete() {
                        d(TAG, "onComplete");
                    }
                });
    }


    // -----------------------------------------------------------------------------------
    // Methods that help wiring up the example (irrelevant to RxJava)

    private void setupLogger() {
        _logs = new ArrayList<>();
        _adapter = new LogAdapter(this, new ArrayList<String>());
        _logsList.setAdapter(_adapter);
    }

    private void log(String logMsg) {

        if (_isCurrentlyOnMainThread()) {
            _logs.add(0, logMsg + "\n(main thread)");
            _adapter.clear();
            _adapter.addAll(_logs);
        } else {
            _logs.add(0, logMsg + "\n(NOT main thread)\n[thread=" + Thread.currentThread().getId() + "]");

            // You can only do below stuff on main thread.
            new Handler(Looper.getMainLooper())
                    .post(
                            () -> {
                                _adapter.clear();
                                _adapter.addAll(_logs);
                            });
        }
    }

    private boolean _isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public void d(String TAG, String msg) {
        d(msg);
    }

    public void d(String msg) {
        log(msg);
        Log.i(TAG, msg);
    }

    public void clear(View view) {
        _logs = new ArrayList<>();
        if (_adapter != null) {
            _adapter.clear();
        }
    }
}
