package com.vector.rxjava2learndemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class NetSimulateActivity extends AppCompatActivity {
    private static final String TAG = NetSimulateActivity.class.getSimpleName();
    private ListView _logsList;
    private ArrayList<String> _logs;
    private LogAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_simulate);
        initLog();
    }

    private void initLog() {
        _logsList = (ListView) findViewById(R.id.lv_log);

        setupLogger();
    }

    //模拟缓存数据源。
    private Observable<List<NewsResultEntity>> getCacheArticle(final long simulateTime) {
        return Observable.create(new ObservableOnSubscribe<List<NewsResultEntity>>() {
            @Override
            public void subscribe(ObservableEmitter<List<NewsResultEntity>> observableEmitter) throws Exception {
                try {
                    d(TAG, "开始加载缓存数据");
                    Thread.sleep(simulateTime);
                    List<NewsResultEntity> results = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        NewsResultEntity entity = new NewsResultEntity();
                        entity.setType("缓存");
                        entity.setDesc("序号=" + i);
                        results.add(entity);
                    }
                    observableEmitter.onNext(results);
                    observableEmitter.onComplete();
                    d(TAG, "结束加载缓存数据");
                } catch (InterruptedException e) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(e);
                    }
                }
            }
        });
    }

    //模拟网络数据源。
    private Observable<List<NewsResultEntity>> getNetworkArticle(final long simulateTime) {
        return Observable.create(new ObservableOnSubscribe<List<NewsResultEntity>>() {
            @Override
            public void subscribe(ObservableEmitter<List<NewsResultEntity>> observableEmitter) throws Exception {
                try {
                    d(TAG, "开始加载网络数据");
                    Thread.sleep(simulateTime);
                    List<NewsResultEntity> results = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        NewsResultEntity entity = new NewsResultEntity();
                        entity.setType("网络");
                        entity.setDesc("序号=" + i);
                        results.add(entity);
                    }
                    observableEmitter.onNext(results);
                    observableEmitter.onComplete();
                    d(TAG, "结束加载网络数据");
                } catch (InterruptedException e) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(e);
                    }
                }
            }
        });
    }

    /**
     * 从控制台的输出可以看到，整个过程是先取读取缓存，等缓存的数据读取完毕之后，才开始请求网络，因此整个过程的耗时为两个阶段的相加，即2500ms
     *
     * @param view
     */
    public void concat(View view) {
        Observable<List<NewsResultEntity>> networkObservable = getNetworkArticle(2000).subscribeOn(Schedulers.io());
        Observable<List<NewsResultEntity>> cacheObservable = getCacheArticle(500).subscribeOn(Schedulers.io());

        //分先后顺序
        Observable.concat(cacheObservable, networkObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());


    }

    /**
     * 它和concat最大的不同就是多个Observable可以同时开始发射数据，如果后一个Observable发射完成后，前一个Observable还有发射完数据，
     * 那么它会将后一个Observable的数据先缓存起来，等到前一个Observable发射完毕后，才将缓存的数据发射出去。
     *
     * @param view
     */
    public void concatEager(View view) {
        Observable<List<NewsResultEntity>> networkObservable = getNetworkArticle(2000).subscribeOn(Schedulers.io());
        Observable<List<NewsResultEntity>> cacheObservable = getCacheArticle(500).subscribeOn(Schedulers.io());


        Observable.concatEager(Arrays.asList(cacheObservable, networkObservable))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    /**
     * 它和concatEager一样，会让多个Observable同时开始发射数据，但是它不需要Observable之间的互相等待，而是直接发送给下游。
     *
     * @param view
     */
    public void merge(View view) {
        Observable<List<NewsResultEntity>> networkObservable = getNetworkArticle(2000).subscribeOn(Schedulers.io());
        Observable<List<NewsResultEntity>> cacheObservable = getCacheArticle(500).subscribeOn(Schedulers.io());

        //不分先后顺序
        Observable.merge(networkObservable, cacheObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    /**
     * 可以看到，在读取缓存的时间大于请求网络时间的时候，仅仅只会展示网络的数据
     *
     * @param view
     */
    public void publish(View view) {

        Observable<List<NewsResultEntity>> networkObservable = getNetworkArticle(2000).subscribeOn(Schedulers.io());
        Observable<List<NewsResultEntity>> cacheObservable = getCacheArticle(500).subscribeOn(Schedulers.io());

        networkObservable
                .publish(network -> {
                    //这里，我们给sourceObservable通过takeUntil传入了另一个otherObservable，它表示sourceObservable在otherObservable发射数据之后，就不允许再发射数据了，
                    // 这就刚好满足了我们前面说的“只要网络源发送了数据，那么缓存源就不应再发射数据”。
                    //之后，我们再用前面介绍过的merge操作符，让两个缓存源和网络源同时开始工作，去取数据。
                    return Observable.merge(network, cacheObservable.takeUntil(network));
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    /**
     * 可以看到，在读取缓存的时间大于请求网络时间的时候，仅仅只会展示网络的数据
     *
     * @param view
     */
    public void publish1(View view) {

        //  但是上面有一点缺陷，就是调用merge和takeUntil会发生两次订阅，
        // 这时候就需要使用publish操作符，它接收一个Function函数，该函数返回一个Observable，
        // 该Observable是对原Observable，也就是上面网络源的Observable转换之后的结果，
        // 该Observable可以被takeUntil和merge操作符所共享，从而实现只订阅一次的效果。


        Observable<List<NewsResultEntity>> networkObservable = getNetworkArticle(2000)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends List<NewsResultEntity>>>() {

                    @Override
                    public ObservableSource<? extends List<NewsResultEntity>> apply(Throwable throwable) throws Exception {
                        Log.d(TAG, "网络请求发生错误throwable=" + throwable);
                        return Observable.never();
                    }
                });



        Observable<List<NewsResultEntity>> cacheObservable = getCacheArticle(5000).subscribeOn(Schedulers.io());

        networkObservable
                .publish(network -> {
                    //这里，我们给sourceObservable通过takeUntil传入了另一个otherObservable，它表示sourceObservable在otherObservable发射数据之后，就不允许再发射数据了，
                    // 这就刚好满足了我们前面说的“只要网络源发送了数据，那么缓存源就不应再发射数据”。
                    //之后，我们再用前面介绍过的merge操作符，让两个缓存源和网络源同时开始工作，去取数据。
                    return Observable.merge(network, cacheObservable.takeUntil(network));
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    /**
     * 发射来自原始Observable的数据，直到第二个Observable发射了一个数据或一个通知，
     *
     * @param view
     */
    public void takeUntil(View view) {

        Observable<List<NewsResultEntity>> networkObservable = getNetworkArticle(2000).subscribeOn(Schedulers.io());
        Observable<List<NewsResultEntity>> cacheObservable = getCacheArticle(500).subscribeOn(Schedulers.io());

        //同时发射，这里networkObservable会发射两次，
        Observable.merge(networkObservable, cacheObservable.takeUntil(networkObservable))
                .subscribe(getObserver());

    }

    @NonNull
    private Observer<List<NewsResultEntity>> getObserver() {
        return new Observer<List<NewsResultEntity>>() {
            @Override
            public void onSubscribe(Disposable d) {
                d("onSubscribe");
            }

            @Override
            public void onNext(List<NewsResultEntity> newsResultEntities) {
                StringBuilder stringBuilder = new StringBuilder();
                for (NewsResultEntity newsResultEntity : newsResultEntities) {
                    stringBuilder.append(newsResultEntity.getType() + newsResultEntity.getDesc() + "\n");

                }
                d("onNext\n" + stringBuilder.toString());
            }

            @Override
            public void onError(Throwable e) {
                d("onError");
            }

            @Override
            public void onComplete() {
                d("onComplete");
            }
        };
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
