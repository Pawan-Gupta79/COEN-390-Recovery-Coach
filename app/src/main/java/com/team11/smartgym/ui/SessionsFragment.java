package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;

public class SessionsFragment extends Fragment {

    private RecyclerView rvSessions;
    private View emptyView;
    private RecyclerView.Adapter<?> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sessions, container, false);

        rvSessions = v.findViewById(R.id.rvSessions);
        emptyView = v.findViewById(R.id.emptyView);

        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));

        // TEMP adapter until real DS-03 bindings (Sprint 2)
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // placeholder for now
                View item = new View(parent.getContext());
                return new RecyclerView.ViewHolder(item) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}

            @Override
            public int getItemCount() {
                return 0; // <-- forces empty state for now
            }
        };

        rvSessions.setAdapter(adapter);

        updateEmpty();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { updateEmpty(); }
            @Override public void onItemRangeInserted(int positionStart, int itemCount) { updateEmpty(); }
            @Override public void onItemRangeRemoved(int positionStart, int itemCount) { updateEmpty(); }
        });

        return v;
    }

    private void updateEmpty() {
        boolean showEmpty = adapter.getItemCount() == 0;
        emptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        rvSessions.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }
}
