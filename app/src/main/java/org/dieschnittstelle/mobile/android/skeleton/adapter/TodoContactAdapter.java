package org.dieschnittstelle.mobile.android.skeleton.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.dieschnittstelle.mobile.android.skeleton.R;
import org.dieschnittstelle.mobile.android.skeleton.model.TodoContact;

import java.util.ArrayList;
import java.util.List;

public class TodoContactAdapter extends RecyclerView.Adapter<TodoContactAdapter.ContactViewHolder> {
    private List<TodoContact> contacts = new ArrayList<>();
    private OnContactActionListener listener;

    public TodoContactAdapter(OnContactActionListener listener) {
        this.listener = listener;
    }

    public interface OnContactActionListener {
        void onDeleteContact(TodoContact contact);
        void onEmailContact(TodoContact contact);
        void onSmsContact(TodoContact contact);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        TodoContact contact = contacts.get(position);
        holder.textViewName.setText(contact.getContactName());

        // Email Button nur anzeigen wenn Email vorhanden
        holder.buttonEmail.setVisibility(
                contact.getContactEmail() != null ? View.VISIBLE : View.GONE);

        // SMS Button nur anzeigen wenn Telefonnummer vorhanden
        holder.buttonSms.setVisibility(
                contact.getContactPhone() != null ? View.VISIBLE : View.GONE);

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteContact(contact);
        });

        holder.buttonEmail.setOnClickListener(v -> {
            if (listener != null) listener.onEmailContact(contact);
        });

        holder.buttonSms.setOnClickListener(v -> {
            if (listener != null) listener.onSmsContact(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setContacts(List<TodoContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        ImageButton buttonDelete, buttonEmail, buttonSms;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewContactName);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteContact);
            buttonEmail = itemView.findViewById(R.id.buttonEmailContact);
            buttonSms = itemView.findViewById(R.id.buttonSmsContact);
        }
    }
}