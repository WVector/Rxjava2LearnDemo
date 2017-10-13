package com.vector.rxjava2learndemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

public class ConnectableObservableActivity extends AppCompatActivity {


    private static final String TAG = ConnectableObservableActivity.class.getSimpleName();

    private Button mBtnSubscribe1;
    private Button mBtnSubscribe2;
    private Button mBtnDispose1;
    private Button mBtnDispose2;
    private Button mBtnDispose;

    private Disposable mConvertDisposable;
    private Observable<Integer> mConvertObservable;
    private Disposable mDisposable1;
    private Disposable mDisposable2;
    private List<Integer> mSubscribe1In = new ArrayList<>();
    private List<Integer> mSubscribe2In = new ArrayList<>();
    private List<Integer> mSourceOut = new ArrayList<>();
    private TextView mTvSubscribe1;
    private TextView mTvSubscribe2;
    private TextView mTvSource;
    private Handler mMainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectable_observable);

        initLog();
        setupDropdown();

        mSourceOut = Collections.synchronizedList(new ArrayList<Integer>());
        mSubscribe1In = Collections.synchronizedList(new ArrayList<Integer>());
        mSubscribe2In = Collections.synchronizedList(new ArrayList<Integer>());
        mTvSubscribe1 = (TextView) findViewById(R.id.tv_subscribe_1);
        mTvSubscribe2 = (TextView) findViewById(R.id.tv_subscribe_2);

        mBtnSubscribe1 = (Button) findViewById(R.id.bt_subscribe_1);
        mBtnSubscribe1.setOnClickListener(v -> startSubscribe1());
        mBtnSubscribe2 = (Button) findViewById(R.id.bt_subscribe_2);
        mBtnSubscribe2.setOnClickListener(v -> startSubscribe2());
        mBtnDispose1 = (Button) findViewById(R.id.bt_dispose_1);
        mBtnDispose1.setOnClickListener(v -> disposeSubscribe1());
        mBtnDispose2 = (Button) findViewById(R.id.bt_dispose_2);
        mBtnDispose2.setOnClickListener(v -> disposeSubscribe2());
        mBtnDispose = (Button) findViewById(R.id.bt_dispose);
        mBtnDispose.setOnClickListener(v -> dispose());


        mTvSource = (TextView) findViewById(R.id.tv_source);
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupDropdown() {
        Spinner spinner = (Spinner) findViewById(R.id.dropdown);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("coldSource()",
                        "cold.publish()",
                        "cold.share()",
                        "cold.publish().autoConnect(2)",
                        "cold.reply(3)"));

        spinner.setAdapter(arrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        initMessageData();
                        createColdSource();
                        break;
                    case 1:
                        initMessageData();
                        createPublishSource();
                        break;
                    case 2:
                        initMessageData();
                        createShareSource();
                        break;
                    case 3:
                        initMessageData();
                        createAutoConnectSource();
                        break;
                    case 4:
                        initMessageData();
                        createReplySource();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }

    /**
     * 初始化信息显示
     */
    private void initMessageData() {

        if (mConvertDisposable != null && !mConvertDisposable.isDisposed()) {
            mConvertDisposable.dispose();
        }
        if (mConvertObservable != null) {
            mConvertObservable = null;
        }


        mSourceOut.clear();
        updateMessage();
    }


    private void createObservable() {
        //createColdSource(); //(1)订阅到cold;
        //createPublishSource(); //(2)订阅到cold.publish();
        //createShareSource(); //(3)订阅到cold.share();
        //createAutoConnectSource(); //(4)订阅到cold.publish().autoConnect(2);
        //createReplySource(); //(5)订阅到cold.reply(3);
    }

    //直接订阅Cold Observable。
    private void createColdSource() {
        mConvertObservable = getSource();
    }

    //.publish()将源Observable转换成为HotObservable，当调用它的connect方法后，无论此时有没有订阅者，源Observable都开始发送数据，订阅者订阅后将可以收到数据，并且订阅者解除订阅不会影响源Observable数据的发射。
    public void createPublishSource() {
        mConvertObservable = getSource().publish();
        mConvertDisposable = ((ConnectableObservable<Integer>) mConvertObservable).connect();
    }

    /**
     * 第一个订阅者订阅到refObservable后，Cold Observable开始发送数据。
     * 之后的订阅者订阅到refObservable后，只能收到在订阅之后Cold Observable发送的数据。
     * 如果一个订阅者取消订阅到refObservable后，假如它是当前refObservable的唯一一个订阅者，那么Cold Observable会停止发送数据；否则，Cold Observable仍然会继续发送数据，其它的订阅者仍然可以收到Cold Observable发送的数据。
     */
    //.share()相当于.publish().refCount()，当有订阅者订阅时，源订阅者会开始发送数据，如果所有的订阅者都取消订阅，源Observable就会停止发送数据。
    private void createShareSource() {
        mConvertObservable = getSource().publish().refCount();
    }

    //.autoConnect在有指定个订阅者时开始让源Observable发送消息，但是订阅者是否取消订阅不会影响到源Observable的发射。
    private void createAutoConnectSource() {
        mConvertObservable = getSource().publish().autoConnect(1, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                mConvertDisposable = disposable;
            }
        });
    }

    /**
     * .reply会让缓存源Observable的N个数据项，当有新的订阅者订阅时，它会发送这N个数据项给它。
     */
    private void createReplySource() {
        mConvertObservable = getSource().replay(3);
        mConvertDisposable = ((ConnectableObservable<Integer>) mConvertObservable).connect();
    }

    private void startSubscribe1() {
        if (mConvertObservable != null && mDisposable1 == null) {
            mDisposable1 = mConvertObservable.subscribe(integer -> {
                d(TAG, "订阅者1收到数据=" + integer);
                mSubscribe1In.add(integer);
                updateMessage();
            });
        }
    }

    private void disposeSubscribe1() {
        if (mDisposable1 != null) {
            mDisposable1.dispose();
            mDisposable1 = null;
            mSubscribe1In.clear();
            updateMessage();
        }
    }

    private void startSubscribe2() {
        if (mConvertObservable != null && mDisposable2 == null) {
            mDisposable2 = mConvertObservable.subscribe(integer -> {
                d(TAG, "订阅者2收到数据=" + integer);
                mSubscribe2In.add(integer);
                updateMessage();
            });
        }
    }

    private void disposeSubscribe2() {
        if (mDisposable2 != null) {
            mDisposable2.dispose();
            mDisposable2 = null;
            mSubscribe2In.clear();
            updateMessage();
        }
    }

    private void dispose() {
        if (mConvertDisposable != null) {
            mConvertDisposable.dispose();
            mConvertDisposable = null;
            mSourceOut.clear();
        }
    }

    private Observable<Integer> getSource() {
        return Observable.create((ObservableOnSubscribe<Integer>) observableEmitter -> {
            try {
                int i = 0;
                while (true) {
                    d(TAG, "源被订阅者发射数据=" + i);
                    mSourceOut.add(i);
                    observableEmitter.onNext(i++);
                    updateMessage();
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).subscribeOn(Schedulers.io());
    }

    private void updateMessage() {
        mMainHandler.post(() -> {
            mTvSource.setText(toMessage("源 Observer 发射历史", mSourceOut));
            mTvSubscribe1.setText(toMessage("订阅者1", mSubscribe1In));
            mTvSubscribe2.setText(toMessage("订阅者2", mSubscribe2In));
        });
    }

    private String toMessage(String prefix, List<Integer> set) {
        String result = prefix + ":    ";
        for (Integer data : set) {
            result = result + data + ",";
        }
        return result;
    }

    // -----------------------------------------------------------------------------------
    // Methods that help wiring up the example (irrelevant to RxJava)
    private void initLog() {
        _logsList = (ListView) findViewById(R.id.lv_log);
        setupLogger();
    }

    private ListView _logsList;
    private ArrayList<String> _logs;
    private LogAdapter _adapter;

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
