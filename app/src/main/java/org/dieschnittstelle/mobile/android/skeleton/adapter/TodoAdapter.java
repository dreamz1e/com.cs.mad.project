package org.dieschnittstelle.mobile.android.skeleton.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.dieschnittstelle.mobile.android.skeleton.R;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> {
    private List<Todo> todoList;

    private TodoRepository todoRepository;


    public TodoAdapter(List<Todo> todos, TodoRepository todoRepository) {
        this.todoList = todos;
        this.todoRepository = todoRepository;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View todoView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new ViewHolder(todoView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        holder.textViewName.setText(todo.getName());
        holder.textViewDescription.setText(todo.getDescription());
        holder.toggleCompleted.setChecked(todo.isDone());

        // Toggle-Button Listener
        holder.toggleCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            todo.setDone(isChecked);
            // Lokale Daten aktualisieren
            todoRepository.updateTodo(todo);
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewName;
        public TextView textViewDescription;
        public ToggleButton toggleCompleted;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewTodoName);
            textViewDescription = itemView.findViewById(R.id.textViewTodoDescription);
            toggleCompleted = itemView.findViewById(R.id.toggleButtonCompleted);
        }
    }
}
