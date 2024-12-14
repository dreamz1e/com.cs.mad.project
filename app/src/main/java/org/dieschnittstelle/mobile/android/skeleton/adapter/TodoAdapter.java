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
        this.todos = todos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todos.get(position);

        holder.textViewName.setText(todo.getName());
        holder.textViewDueDate.setText(todo.getExpiry() != 0 ? 
            dateFormat.format(new Date(todo.getExpiry())) : "Kein Fälligkeitsdatum");
        holder.checkBoxCompleted.setChecked(todo.isDone());
        holder.toggleButtonFavorite.setChecked(todo.isFavourite());

        // Visuelle Hervorhebung überfälliger Todos
        if (!todo.isDone() && todo.getExpiry() != 0 && 
            Instant.ofEpochMilli(todo.getExpiry()).isBefore(Instant.now())) {
            // Ändere die Textfarbe für überfällige Todos
            holder.textViewName.setTextColor(Color.RED);
            holder.textViewDueDate.setTextColor(Color.RED);
            // Optional: Hintergrund hervorheben
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE")); // Leichtes Rot
        } else {
            // Standardfarben
            holder.textViewName.setTextColor(Color.BLACK);
            holder.textViewDueDate.setTextColor(Color.DKGRAY);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // KlickListener für die Detailansicht
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(todo);
            }
        });

        // Favourite-Toggle
        holder.toggleButtonFavorite.setOnClickListener(v -> {
            boolean isFavorite = holder.toggleButtonFavorite.isChecked();
            todo.setFavourite(isFavorite);
            if (listener != null) {
                listener.onFavoriteToggle(todo, isFavorite);
            }
        });

        // Completed-Toggle
        holder.checkBoxCompleted.setOnClickListener(v -> {
            boolean isCompleted = holder.checkBoxCompleted.isChecked();
            todo.setDone(isCompleted);
            if (listener != null) {
                listener.onCompletedToggle(todo, isCompleted);
            }
        });
    }

    @Override
    public int getItemCount() {
        return todos.size();
    }

    public class TodoViewHolder extends RecyclerView.ViewHolder {

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
