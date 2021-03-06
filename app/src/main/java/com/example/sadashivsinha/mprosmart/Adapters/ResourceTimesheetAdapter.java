package com.example.sadashivsinha.mprosmart.Adapters;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sadashivsinha.mprosmart.ModelLists.SubcontractorList;
import com.example.sadashivsinha.mprosmart.R;
import com.example.sadashivsinha.mprosmart.SharedPreference.PreferenceManager;
import com.example.sadashivsinha.mprosmart.Utils.DatePickerFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by saDashiv sinha on 11-Aug-16.
 */
public class ResourceTimesheetAdapter extends RecyclerView.Adapter<ResourceTimesheetAdapter.MyViewHolder> {

    public List<SubcontractorList> subcontractorList;
    TextView textViewDate;
    Context mContext;
    PreferenceManager pm;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView text_line_no;
        public Button editBtn;
        public EditText text_wbs, text_activities, text_res_name, text_total_hours;
        TextView text_date;
        ProgressDialog pDialog;
        String lineNo;
        Snackbar snackbar;
        LinearLayout resource_name_layout;

        public MyViewHolder(final View view) {
            super(view);

            pm = new PreferenceManager(view.getContext());

            text_line_no = (TextView) view.findViewById(R.id.text_line_no);
            text_wbs = (EditText) view.findViewById(R.id.text_wbs);
            text_activities = (EditText) view.findViewById(R.id.text_activities);
            text_res_name = (EditText) view.findViewById(R.id.text_res_name);
            text_date = (TextView) view.findViewById(R.id.text_date);
            text_total_hours = (EditText) view.findViewById(R.id.text_total_hours);

            editBtn = (Button) view.findViewById(R.id.editBtn);

            resource_name_layout = (LinearLayout) view.findViewById(R.id.resource_name_layout);

            resource_name_layout.setVisibility(View.GONE);

            editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(text_res_name.isEnabled()
                            && text_total_hours.isEnabled() && text_date.isEnabled())
                    {
                        text_res_name.setEnabled(false);
                        text_date.setEnabled(false);
                        text_total_hours.setEnabled(false);

                        editBtn.setText("EDIT");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            editBtn.setBackgroundTintList(view.getContext().getResources().getColorStateList(R.color.colorPrimary));
                        }
                        pDialog = new ProgressDialog(view.getContext());
                        pDialog.setMessage("Saving Data ...");
                        pDialog.setIndeterminate(false);
                        pDialog.setCancelable(true);
                        pDialog.show();

                        class MyTask extends AsyncTask<Void, Void, Void> {

                            @Override
                            protected Void doInBackground(Void... params) {
                                savingData();
                                return null;
                            }
                        }

                        new MyTask().execute();
                    }

                    else
                    {
                        text_res_name.setEnabled(true);
                        text_date.setEnabled(true);
                        text_total_hours.setEnabled(true);


                        text_res_name.requestFocus();
                        editBtn.setText("SAVE");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            editBtn.setBackgroundTintList(view.getContext().getResources().getColorStateList(R.color.success_green));
                        }
                    }
                }

                public void savingData()
                {
                    JSONObject object = new JSONObject();

                    try {

                        Date tradeDate = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).parse(text_date.getText().toString());
                        String dateToSend = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(tradeDate);

                        object.put("name","");
                        object.put("date",dateToSend);
                        object.put("totalHours",text_total_hours.getText().toString());

                        Log.d("resource object", object.toString());

                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }

                    RequestQueue requestQueue = Volley.newRequestQueue(view.getContext());

                    lineNo = text_line_no.getText().toString();
                    String url =pm.getString("SERVER_URL")  + "/putResourceLineItem?resourceLineItemsId=\""+lineNo + "\"";

                    JsonObjectRequest jor = new JsonObjectRequest(Request.Method.PUT, url, object,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {

                                        Toast.makeText(view.getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                                        pDialog.dismiss();

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    //response success message display
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e("Volley","Error");
                                    pDialog.dismiss();
                                }
                            }
                    );
                    requestQueue.add(jor);
                }
            });
        }
    }
    public ResourceTimesheetAdapter(List<SubcontractorList> subcontractorList) {
        this.subcontractorList = subcontractorList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_resource_timesheet, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        SubcontractorList items = subcontractorList.get(position);
        holder.text_line_no.setText(String.valueOf(items.getText_line_no()));
        holder.text_wbs.setText(items.getText_wbs());
        holder.text_activities.setText(items.getText_activities());
        holder.text_res_name.setText(String.valueOf(items.getText_res_name()));
        holder.text_total_hours.setText(items.getText_total_hours());

        Date tradeDate = null;
        try {
            tradeDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(items.getText_date());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String formattedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(tradeDate);

        holder.text_date.setText(formattedDate);

        holder.text_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                textViewDate = holder.text_date;
                mContext = holder.itemView.getContext();
                showDatePicker(holder.itemView.getContext());

            }
        });

    }

    private void showDatePicker(Context context) {
        DatePickerFragment date = new DatePickerFragment();
        /**
         * Set Up Current Date Into dialog
         */
        Calendar calender = Calendar.getInstance();
        Bundle args = new Bundle();
        args.putInt("year", calender.get(Calendar.YEAR));
        args.putInt("month", calender.get(Calendar.MONTH));
        args.putInt("day", calender.get(Calendar.DAY_OF_MONTH));
        date.setArguments(args);
        /**
         * Set Call back to capture selected date
         */
        date.setCallBack(ondate);
        date.show(((Activity) context).getFragmentManager(), "Date Picker");

    }

    DatePickerDialog.OnDateSetListener ondate = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {

            String newDate = String.valueOf(dayOfMonth) + "-" + String.valueOf(monthOfYear+1)
                    + "-" + String.valueOf(year);

            textViewDate.setText(newDate);
        }
    };


    @Override
    public int getItemCount()
    {
        return subcontractorList.size();
    }
}

