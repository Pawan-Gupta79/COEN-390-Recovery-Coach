package com.team11.smartgym.ui;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepo;
import com.team11.smartgym.databinding.FragmentSessionsBinding;

import java.util.ArrayList;
import java.util.List;

public class SessionsFragment extends Fragment {
    private FragmentSessionsBinding b;
    private final List<Session> data = new ArrayList<>();
    private Adapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSessionsBinding.inflate(inflater, container, false);
        adapter = new Adapter(data);
        b.rv.setAdapter(adapter);
        load();
        return b.getRoot();
    }

    private void load() {
        new Thread(() -> {
            List<Session> list = new SessionRepo(requireContext()).list();
            requireActivity().runOnUiThread(() -> {
                data.clear(); data.addAll(list); adapter.notifyDataSetChanged();
            });
        }).start();
    }

    static class Adapter extends RecyclerView.Adapter<VH> {
        private final List<Session> list;
        Adapter(List<Session> list) { this.list = list; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Session s = list.get(pos);
            String title = "Session • " + android.text.format.DateFormat.format("MMM d, h:mm a", s.startedAt);
            long end = s.endedAt == 0 ? System.currentTimeMillis() : s.endedAt;
            String meta = "Avg " + s.avgBpm + " bpm • Max " + s.maxBpm + " bpm • " +
                    DateUtils.formatElapsedTime((end - s.startedAt) / 1000);
            h.t1.setText(title);
            h.t2.setText(meta);
        }
        @Override public int getItemCount() { return list.size(); }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        VH(@NonNull View v) { super(v); t1 = v.findViewById(R.id.tvTitle); t2 = v.findViewById(R.id.tvMeta); }
    }
}