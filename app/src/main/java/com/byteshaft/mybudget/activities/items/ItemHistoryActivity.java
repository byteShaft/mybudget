package com.byteshaft.mybudget.activities.items;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.mybudget.AppGlobals;
import com.byteshaft.mybudget.R;
import com.byteshaft.mybudget.Utils.Helpers;
import com.byteshaft.mybudget.activities.goals.AdjustDepositActivity;
import com.byteshaft.mybudget.adapters.ItemHistoryAdapter;
import com.byteshaft.mybudget.containers.LineItem;
import com.byteshaft.mybudget.database.DBHelper;

import java.util.ArrayList;

/*
    Displays expense history and overview for a specific line item.
 */

public class ItemHistoryActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private LineItem myItem;
    private float itemSpent;
    private DBHelper myDb;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_history);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(1000);
                Toast toast = Toast.makeText(getApplicationContext(), v.getContentDescription(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
                toast.show();
                return true;
            }
        };
        mRecyclerView = (RecyclerView) findViewById(R.id.item_history_recycler);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (AppGlobals.getsCurrentMonthYear() != null) {
            myDb = new DBHelper(getApplicationContext(), AppGlobals.getsCurrentMonthYear() + ".db");
        } else if (AppGlobals.getDatePickerState() || AppGlobals.getDpCurrentMonthExist()) {
            myDb = new DBHelper(getApplicationContext(), AppGlobals.getDatePickerValues() + ".db");
        } else {
            myDb = new DBHelper(getApplicationContext(), Helpers.getTimeStamp("MMM_yyyy") + ".db");
        }

        Bundle b = getIntent().getExtras();

        if (b != null) {
            name = b.getString("ITEM_NAME");
            getSupportActionBar().setTitle(name);
            myItem = myDb.getLineItem(name);
            if (myItem != null) {
                setOverview();
                setHistory(name);
            }
        } else {
            getSupportActionBar().setTitle("Item Name Not Found");
            Context context = getApplicationContext();
            CharSequence text = "Item name was not provided";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setOverview();
        setHistory(name);
    }

    /*
        Sets overview card
     */
    public void setOverview() {
        myItem = myDb.getLineItem(name);
        itemSpent = myDb.getItemSpent(name);
        TextView budgeted = (TextView) findViewById(R.id.budgeted);
        TextView spent = (TextView) findViewById(R.id.spent);
        TextView remaining = (TextView) findViewById(R.id.remaining);
        budgeted.setText("Budgeted: " + Helpers.getCurrency(myItem.getBudget()));
        spent.setText("Spent: " + Helpers.getCurrency(itemSpent));
        remaining.setText("Remaining: " + Helpers.getCurrency(myItem.getBudget() - itemSpent));
    }

    /*
        Set main content - RecyclerView of expenses
     */
    public void setHistory(String name) {
        TextView history = (TextView) findViewById(R.id.item_history_placeholder);
        ArrayList myHistory = myDb.getExpenseHistory(name);
        if (myHistory.size() != 0) {
            history.setVisibility(View.GONE);

            RecyclerView.Adapter mAdapter = new ItemHistoryAdapter(myHistory);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            history.setVisibility(View.VISIBLE);
            mRecyclerView.setAdapter(null);
        }
    }

    public void onAddExpenseClick(View v) {
        Intent intent = new Intent(this, AddExpenseActivity.class);
        intent.putExtra("ITEM_NAME", name);
        startActivity(intent);
    }

    public void onOverviewClick(View v) {
        int REQUEST_ITEM_ADJUSTMENT = 0;
        Intent intent = new Intent(this, AdjustItemActivity.class);
        intent.putExtra("ITEM_NAME", name);
        intent.putExtra("ITEM_BUDGET", myItem.getBudget());
        intent.putExtra("ITEM_SPENT", itemSpent);
        startActivityForResult(intent, REQUEST_ITEM_ADJUSTMENT);
    }

    public void onHistoryClick(View v) {
        TableLayout table = (TableLayout) v;
        String expName = ((TextView) table.findViewById(R.id.history_name)).getText().toString();
        String expDate = ((TextView) table.findViewById(R.id.history_date)).getText().toString();
        float expAmount = trimExpenseAmount(((TextView) table.findViewById(R.id.history_amount)).getText().toString());

        if (expName.contains("Deposit")) {

            Intent intent = new Intent(this, AdjustDepositActivity.class);
            intent.putExtra("DEPOSIT_NAME", expName);
            intent.putExtra("DEPOSIT_DATE", expDate);
            intent.putExtra("DEPOSIT_AMOUNT", expAmount);
            intent.putExtra("ITEM_NAME", name);
            intent.putExtra("ITEM_REMAINING", (myItem.getBudget() - itemSpent));
            startActivity(intent);

        } else {
            Intent intent = new Intent(this, AdjustExpenseActivity.class);
            intent.putExtra("EXPENSE_NAME", expName);
            intent.putExtra("EXPENSE_DATE", expDate);
            intent.putExtra("EXPENSE_AMOUNT", expAmount);
            intent.putExtra("ITEM_NAME", name);
            intent.putExtra("ITEM_REMAINING", (myItem.getBudget() - itemSpent));
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            name = data.getExtras().getString("ITEM_NAME");

        }
    }

    public float trimExpenseAmount(String str) {
        String[] noEuro = str.split("€");
        return Float.parseFloat(noEuro[0].replaceAll("\\s", "").replaceAll(",", ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.on_make_depo:
                Intent intent = new Intent(this, MakeDepositActivity.class);
                intent.putExtra("ITEM_NAME", name);
                startActivity(intent);
                break;
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
