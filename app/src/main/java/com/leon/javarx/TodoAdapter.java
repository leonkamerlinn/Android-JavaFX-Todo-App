package com.leon.javarx;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.jakewharton.rxbinding2.widget.RxCompoundButton;

import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoHolder>{


    private final Activity mActivity;
    List<Todo> listTodo = Collections.emptyList();

    // the Action to get called for onNext() of the check changed Subscription
    Consumer<Todo> toggleTodoConsumer;

    public Consumer<List<Todo>> listTodoConsumer = todos -> {
        listTodo = todos;
        notifyDataSetChanged();
    };


    public TodoAdapter(Activity activity, Consumer<Todo> checkChangedSubscriber) {
        mActivity = activity;
        toggleTodoConsumer = checkChangedSubscriber;
    }

    @Override
    public TodoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TodoHolder(LayoutInflater.from(mActivity).inflate(R.layout.item_todo, parent, false));
    }

    @Override
    public int getItemCount() {
        return listTodo.size();
    }

    @Override
    public void onBindViewHolder(TodoHolder holder, int position) {
        final Todo todo = listTodo.get(position);
        holder.checkbox.setText(todo.description);
        holder.checkbox.setChecked(todo.isCompleted);

        /* Subscribe to the changes of the CheckBox. We skip the first one because it gets
            called with the initial value, we only want to take action on changes.
         */
        holder.checkBoxDisposable = RxCompoundButton.checkedChanges(holder.checkbox)
                .skip(1)
                .map(aBoolean -> {
                    todo.isCompleted = !todo.isCompleted;
                    return todo;
                })
                .subscribe(toggleTodoConsumer); // subscribe with each bind
    }

    @Override
    public void onViewDetachedFromWindow(TodoHolder holder) {
        super.onViewDetachedFromWindow(holder);
        // unsubscribe if we are being removed
        holder.checkBoxDisposable.dispose();
    }




    public class TodoHolder extends RecyclerView.ViewHolder {

        public CheckBox checkbox;
        public Disposable checkBoxDisposable;

        public TodoHolder(View itemView) {
            super(itemView);
            checkbox = (CheckBox) itemView;
        }
    }
}
