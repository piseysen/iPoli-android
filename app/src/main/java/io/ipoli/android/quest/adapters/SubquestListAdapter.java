package io.ipoli.android.quest.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ipoli.android.R;
import io.ipoli.android.quest.data.Subquest;

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 4/28/16.
 */
public class SubquestListAdapter extends RecyclerView.Adapter<SubquestListAdapter.ViewHolder> {
    protected Context context;
    protected final Bus evenBus;
    protected List<Subquest> subquests;

    public SubquestListAdapter(Context context, Bus evenBus, List<Subquest> subquests) {
        this.context = context;
        this.evenBus = evenBus;
        this.subquests = subquests;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.subquest_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Subquest sq = subquests.get(holder.getAdapterPosition());

        holder.name.setText(sq.getName());

        holder.check.setOnCheckedChangeListener(null);
        holder.check.setChecked(sq.isCompleted());
        if(sq.isCompleted()) {
            holder.check.setEnabled(false);
        } else {
            holder.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    sq.setCompleted(true);
                } else {
                    sq.setCompleted(false);
                }
            });
            holder.itemView.setOnClickListener(view -> {
                CheckBox cb = holder.check;
                cb.setChecked(!cb.isChecked());

            });
        }
    }

    @Override
    public int getItemCount() {
        return subquests.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.subquest_check)
        CheckBox check;

        @BindView(R.id.subquest_name)
        TextView name;

        public ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }
}
