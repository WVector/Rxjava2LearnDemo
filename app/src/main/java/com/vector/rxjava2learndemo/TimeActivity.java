package com.vector.rxjava2learndemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TimeActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);
        initLog();
    }

    public void time(View view) {
        Observable.timer(1, TimeUnit.SECONDS, Schedulers.newThread())
                .doOnEach(longNotification -> {
                    d("发射");
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    public void interval(View view) {
        Observable.interval(0, 2, TimeUnit.SECONDS, Schedulers.newThread())
                .doOnEach(longNotification -> {
                    d("发射");
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    /**
     * 当它接受一个时间段时，每当原始的Observable发射了一个数据项时，它就启动一个定时器，等待指定的时间后再将这个数据发射出去，
     * 因此表现为发射的数据项进行了平移，但是它只会平移onNext/onComplete，对于onError，它会立即发射出去，并且丢弃之前等待发射的onNext事件
     *
     * @param view
     */
    public void delay(View view) {

        Observable.interval(1, TimeUnit.SECONDS)
                .take(5)
                .doOnNext(aLong -> {
                    d("发射");
                }).delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());

    }

    public void dispose(View view) {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }

    private Disposable mDisposable;

    @NonNull
    private Observer<Long> getObserver() {
        return new Observer<Long>() {


            @Override
            public void onSubscribe(Disposable d) {
                mDisposable = d;
                d("onSubscribe");
            }

            @Override
            public void onNext(Long number) {
                d("onNext\n" + number);
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
    private void initLog() {
        _logsList = (ListView) findViewById(R.id.lv_log);
        setupLogger();
    }

    private static final String TAG = TimeActivity.class.getSimpleName();
    private ListView _logsList;
    private ArrayList<String> _logs;
    private LogAdapter _adapter;

    private void setupLogger() {
        _logs = new ArrayList<>();
        _adapter = new LogAdapter(this, new ArrayList<String>());
        _logsList.setAdapter(_adapter);
    }

    private void log(String logMsg) {

        DateFormat timeInstance = SimpleDateFormat.getTimeInstance();
        String time = timeInstance.format(new Date());
        if (_isCurrentlyOnMainThread()) {
            _logs.add(0, time + "\n" + logMsg + "\n(main thread)");
            _adapter.clear();
            _adapter.addAll(_logs);
        } else {
            _logs.add(0, time + "\n" + logMsg + "\n(NOT main thread)\n[thread=" + Thread.currentThread().getId() + "]");

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
