package com.leon.javarx;

import android.annotation.SuppressLint;
import android.arch.core.util.Function;
import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.leon.javarx.room.AppDatabase;
import com.leon.javarx.room.User;

import java.util.List;
import java.util.Observer;
import java.util.function.Consumer;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;



public class MainActivity extends AppCompatActivity implements LifecycleRegistryOwner {

    private static final String LIST = "todoList";
    private static final String FILTER = "filter";
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    TodoList todoList;
    int filterPosition;


    private Toolbar toolbar;
    private Spinner spinner;
    private EditText addInput;
    private RecyclerView recyclerView;
    private Button btnAddTodo;

    // used to handle unsubscription during teardown of Activity
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toolbar = findViewById(R.id.toolbar);
        spinner = findViewById(R.id.spinner);
        addInput = findViewById(R.id.add_todo_input);
        recyclerView = findViewById(R.id.recyclerview);
        btnAddTodo = findViewById(R.id.btn_add_todo);


        setSupportActionBar(toolbar);
        // retrieve the saved todoList and filter or use defaults
        if (savedInstanceState == null) {
            todoList = new TodoList();

            // add some sample items
            Observable<String> stringObservable = Observable.just("Hello", "Hello World", "Sample 1", "Sample 2", "Sample 3");
            stringObservable.subscribe(todoList.addTodoConsumer);



            filterPosition = FilterPositions.ALL;
        } else {
            todoList = new TodoList(savedInstanceState.getString(LIST));
            filterPosition = savedInstanceState.getInt(FILTER);
        }

        /*
            The adapter listens to changes from the todoList Observable.
            The todoList listens to changes from the Adapter.
         */
        TodoAdapter recyclerViewAdapter = new TodoAdapter(this, todoList.toggleTodoConsumer);

        // setup the todoList with the adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(recyclerViewAdapter);


        Observable<Integer> spinnerObservable = RxAdapterView.itemSelections(spinner);
        Observable<TodoList> todoListObservable = todoList.asObservable();
        BiFunction<Integer, TodoList, List<Todo>> spinnerTodoListBiFunction = (integer, todoList) -> {
            switch (integer) {
                case FilterPositions.INCOMPLETE:
                    return this.todoList.getIncomplete();
                case FilterPositions.COMPLETE:
                    return this.todoList.getComplete();
                default:
                    return this.todoList.getAll();
            }
        };

        // combine filter and todolist
        Disposable spinnerTodoListCompositeDisposable = Observable.combineLatest(spinnerObservable, todoListObservable, spinnerTodoListBiFunction)
                .subscribe(recyclerViewAdapter.listTodoConsumer);
        compositeDisposable.add(spinnerTodoListCompositeDisposable);


        Disposable buttonAddDisposable = RxView.clicks(btnAddTodo)
                .map(o -> addInput.getText().toString())
                .filter(s -> {
                    addInput.setText("");
                    findViewById(R.id.add_todo_container).requestFocus();
                    dismissKeyboard();
                    return !TextUtils.isEmpty(s);
                })
                .subscribe(todoList.addTodoConsumer);


        compositeDisposable.add(buttonAddDisposable);


        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"All", "Incomplete", "Completed"}));
        spinner.setSelection(filterPosition);


        mIsDatabaseCreated.setValue(false);
        //new CreateDatabaseTask().execute();

        mIsDatabaseCreated.observeForever(aBoolean -> System.out.println("value: "+aBoolean));




        Flowable<List<User>> source = Flowable.fromCallable(() -> {


            getApplicationContext().deleteDatabase(AppDatabase.DATABASE_NAME);
            AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, AppDatabase.DATABASE_NAME).build();
            User user = new User("Leon", "Kamerlin");
            db.userDao().insertAll(user, user);
            List<User> users = db.userDao().getAll();


            return users;
        });

        source.subscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(users -> {
                   System.out.println(users);
                   mIsDatabaseCreated.setValue(true);
                });
    }




    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(LIST, todoList.toString());
        outState.putInt(FILTER, spinner.getSelectedItemPosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(addInput.getWindowToken(), 0);
    }

    @Override
    public LifecycleRegistry getLifecycle() {
        return mRegistry;
    }






    private class CreateDatabaseTask extends AsyncTask<String, Integer, AppDatabase> {

        public CreateDatabaseTask() {
            mIsDatabaseCreated.setValue(false);
        }


        @Override
        protected AppDatabase doInBackground(String... strings) {
            getApplicationContext().deleteDatabase("database-name");
            AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database-name").build();
            User user = new User("Leon", "Kamerlin");
            db.userDao().insertAll(user, user);
            System.out.println(db.userDao().getAll());
            return db;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(AppDatabase appDatabase) {
            super.onPostExecute(appDatabase);
            mIsDatabaseCreated.setValue(true);


        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

}








