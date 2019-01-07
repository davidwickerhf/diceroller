package io.github.davidwickerhf.diceroller;


//Todo   REMINDERS

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.github.davidwickerhf.diceroller.adapters.SettingAdapter;
import io.github.davidwickerhf.diceroller.settingDatabase.Setting;
import io.github.davidwickerhf.diceroller.settingDatabase.SettingsViewModel;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    //todo DATABASE
    private SettingsViewModel settingsViewModel;
    private SettingAdapter adapter;


    //TODO DEFAULT ELEMENTS
    private Snackbar mSnackbar;
    private CoordinatorLayout coordinatorLayout;

    //TODO VIEWS
    androidx.appcompat.widget.Toolbar dashboardToolbar;
    FloatingActionButton dashboardFab;
    TextView addSettingTextView;
    //TODO VARIABLES
    private static final int ADD_SETTING_REQUEST = 1;
    private Setting recoveredSetting;
    public int selectedItemPosition;
    private String[] optionList;
    private int itemCount;


    //todo Interface for sharing data ( get Settings Information from AddSettingActivity)
    private FragmentDashboardListener listener;

    public interface FragmentDashboardListener {
        void onInputASent(int maxNumber, ArrayList<String> items, int position, String title, View itemView);
    }


    private DashboardItemPositionListener itemListener;

    public interface DashboardItemPositionListener {
        void onEditInputASent(int position);
    }


    //TODO ON CREATE
    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        coordinatorLayout = (CoordinatorLayout) fragmentView.findViewById(R.id.coordinatorLayout);

        //todo Toolbar
        dashboardToolbar = fragmentView.findViewById(R.id.dashboard_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(dashboardToolbar);
        getActivity().setTitle(R.string.dashboard_title);


        //TODO FLOATING ACTION BUTTON
        dashboardFab = (FloatingActionButton) fragmentView.findViewById(R.id.dashboard_fab);
        dashboardFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                optionList = new String[]{getString(R.string.dialog_add_custom_setting), getString(R.string.dialog_add_setting_by_id)};
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity());
                mBuilder.setTitle(R.string.dialog_add_setting_title);
                mBuilder.setSingleChoiceItems(optionList, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selected) {
                        if (selected == 0) {
                            Intent intent = new Intent(getActivity(), AddSettingActivity.class);
                            getActivity().startActivityForResult(intent, ADD_SETTING_REQUEST);
                            dialog.dismiss();
                        } else {
                            Intent intent = new Intent(getActivity(), AddSettingByID.class);
                            getActivity().startActivityForResult(intent, ADD_SETTING_REQUEST);
                            dialog.dismiss();
                        }

                    }
                });
                AlertDialog chooseOption = mBuilder.create();
                chooseOption.show();
            }
        });

        addSettingTextView = fragmentView.findViewById(R.id.dashboard_fragment_ask_to_add_setting_text_view);
        //todo View Model
        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        settingsViewModel.getAllSettings().observe(this, new Observer<List<Setting>>() {
            @Override
            public void onChanged(@Nullable List<Setting> settings) {
                adapter.setSettings(settings);
                itemCount = adapter.getItemCount();

                if (itemCount < 2){
                    addSettingTextView.setVisibility(View.VISIBLE);
                } else{
                    addSettingTextView.setVisibility(View.INVISIBLE);
                }
            }
        });

        //todo Retrieve Selected Item from Previous session
        if (getArguments() != null) {
            selectedItemPosition = getArguments().getInt("selectedItemPosition");
        }


        //todo Recycler View:
        final RecyclerView recyclerView = fragmentView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        recyclerView.setHasFixedSize(true);
        // Set Adapter
        adapter = new SettingAdapter();
        recyclerView.setAdapter(adapter);


        //todo Interface - Returns Setting Position when Clicking Edit Icon - From Adapter to Dashboard to MainActivity
        adapter.setOnEditItemClickLister(new SettingAdapter.OnEditItemClickListener() {
            @Override
            public void onEditItemClick(int position) {
                itemListener.onEditInputASent(position);
            }
        });


        //todo Interface for returning clicked item From SETTING ADAPTER - Than passes setting info to MainActivity
        adapter.setOnItemClickLister(new SettingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, View itemView) {

                int maxNum = adapter.getSettingAt(position).getMaxDiceSum();
                ArrayList<String> items = new ArrayList<>();
                String title = adapter.getSettingAt(position).getTitle();
                if (adapter.getSettings().get(position).getItems() != null) {
                    items.addAll(adapter.getSettings().get(position).getItems());
                    Log.d("HomeFragment", "Item List in DashboardFragment is:" + items.toString());
                }
                Snackbar.make(coordinatorLayout, "Setting selected", Snackbar.LENGTH_LONG).show();

                listener.onInputASent(maxNum, items, position, title, itemView);
            }
        });


        //todo Make Recycler Swipable
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                recoveredSetting = adapter.getSettingAt(viewHolder.getAdapterPosition());
                settingsViewModel.delete(adapter.getSettingAt(viewHolder.getAdapterPosition()));

                mSnackbar = Snackbar
                        .make(coordinatorLayout, "Setting deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                settingsViewModel.insert(recoveredSetting);
                                Snackbar snackbar1 = Snackbar.make(coordinatorLayout, "Setting restored", Snackbar.LENGTH_SHORT);
                                snackbar1.show();
                            }
                        });

                mSnackbar.show();
            }
        }).attachToRecyclerView(recyclerView);

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (FragmentDashboardListener) context;
        itemListener = (DashboardItemPositionListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        itemListener = null;
    }
}