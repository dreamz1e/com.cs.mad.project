package org.dieschnittstelle.mobile.android.skeleton.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.dieschnittstelle.mobile.android.skeleton.R;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<Todo> todos = new ArrayList<>();
    private Context context;
    private TodoRepository todoRepository;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(Todo todo);
        void onFavoriteToggle(Todo todo, boolean isFavorite);
        void onCompletedToggle(Todo todo, boolean isCompleted);
    }

    public TodoAdapter(Context context, TodoRepository todoRepository, OnItemClickListener listener) {
        this.context = context;
        this.todoRepository = todoRepository;
        this.listener = listener;
    }

    public void setTodos(List<Todo> todos) {
        this.todos = new ArrayList<>(todos); // Erstelle eine Kopie der Liste
        notifyDataSetChanged();
    }

    public int getTodoPosition(Todo todo) {
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).getId() == todo.getId()) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false);
        TodoViewHolder holder = new TodoViewHolder(view);

        // Setze die Click-Listener im onCreateViewHolder
        holder.checkBoxCompleted.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                Todo todo = todos.get(position);
                boolean isChecked = holder.checkBoxCompleted.isChecked();
                todo.setDone(isChecked);
                listener.onCompletedToggle(todo, isChecked);
            }
        });

        holder.toggleButtonFavorite.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                Todo todo = todos.get(position);
                boolean isChecked = holder.toggleButtonFavorite.isChecked();
                todo.setFavourite(isChecked);
                listener.onFavoriteToggle(todo, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(todos.get(position));
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todos.get(position);

        // Setze die Werte ohne Listener
        holder.checkBoxCompleted.setOnClickListener(null);
        holder.toggleButtonFavorite.setOnClickListener(null);

        // Setze die Werte
        holder.textViewName.setText(todo.getName());
        holder.textViewDueDate.setText(todo.getExpiry() != 0 ? 
            dateFormat.format(new Date(todo.getExpiry())) : "Kein Fälligkeitsdatum");
        holder.checkBoxCompleted.setChecked(todo.isDone());
        holder.toggleButtonFavorite.setChecked(todo.isFavourite());

        // Visuelle Hervorhebung überfälliger Todos
        updateVisualState(holder, todo);

        // Setze die Click-Listener
        holder.checkBoxCompleted.setOnClickListener(v -> {
            boolean isChecked = ((CheckBox) v).isChecked();
            if (listener != null) {
                listener.onCompletedToggle(todo, isChecked);
            }
        });

        holder.toggleButtonFavorite.setOnClickListener(v -> {
            boolean isChecked = ((ToggleButton) v).isChecked();
            if (listener != null) {
                listener.onFavoriteToggle(todo, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(todo);
            }
        });
    }

    private void updateVisualState(TodoViewHolder holder, Todo todo) {
        if (!todo.isDone() && todo.getExpiry() != 0 && 
            Instant.ofEpochMilli(todo.getExpiry()).isBefore(Instant.now())) {
            holder.textViewName.setTextColor(Color.RED);
            holder.textViewDueDate.setTextColor(Color.RED);
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE"));
        } else {
            holder.textViewName.setTextColor(Color.BLACK);
            holder.textViewDueDate.setTextColor(Color.DKGRAY);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return todos.size();
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewDueDate;
        ToggleButton toggleButtonFavorite;
        CheckBox checkBoxCompleted;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewDueDate = itemView.findViewById(R.id.textViewDueDate);
            toggleButtonFavorite = itemView.findViewById(R.id.toggleButtonFavorite);
            checkBoxCompleted = itemView.findViewById(R.id.checkBoxCompleted);
        }
    }
}