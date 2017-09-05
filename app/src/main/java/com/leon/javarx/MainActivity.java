package com.leon.javarx;

import android.content.Context;
import android.os.Bundle;
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

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import java.util.List;
import java.util.Observer;
import java.util.function.Consumer;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;


public class MainActivity extends AppCompatActivity {

    private static final String LIST = "todoList";
    private static final String FILTER = "filter";

    TodoList todoList;
    int filterPosition;


    private Toolbar toolbar;
    private Spinner spinner;
    private EditText addInput;
    private RecyclerView recyclerView;
    private Button btnAddTodo;

    // used to handle unsubscription during teardown of Activity
    CompositeDisposable compositeDisposable = new CompositeDisposable();


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


        Observable<Integer> spinnerObservable = RxAdapterView.itemSelections(spinner).skip(1);
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
        Observable<List<Todo>> listTodoObservable = Observable.combineLatest(spinnerObservable, todoListObservable, spinnerTodoListBiFunction);
        compositeDisposable.add(listTodoObservable.subscribe(recyclerViewAdapter.listTodoConsumer));


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

}
