package com.example.sadashivsinha.mprosmart.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sadashivsinha.mprosmart.R;
import com.example.sadashivsinha.mprosmart.SharedPreference.PreferenceManager;
import com.example.sadashivsinha.mprosmart.Utils.AppController;
import com.example.sadashivsinha.mprosmart.Utils.Communicator;
import com.example.sadashivsinha.mprosmart.Utils.ConnectionDetector;
import com.example.sadashivsinha.mprosmart.font.HelveticaRegular;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class InvoiceCreate extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    EditText text_vendor_invoice_no, text_vendor;
    ProgressDialog pDialog, pDialog1;
    String currentProjectNo, currentUserName, vendorId, receiptId;
    HelveticaRegular date_created;
    public static final String TAG = InvoiceCreate.class.getSimpleName();
    JSONArray dataArray;
    JSONObject dataObject;
    Spinner spinnerReceipt, spinner_po;
    String[] purchaseOrdersArray, vendorIdArray, receiptArray;
    String poId,purchaseReceiptId;
    String matchPurchaseOrderId;
    ConnectionDetector cd;
    Boolean isInternetPresent = false;
    String po_url, currentVendor;
    PreferenceManager pm;
    String receiptUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_create);

        spinnerReceipt = (Spinner) findViewById(R.id.spinnerReceipt);

        pm = new PreferenceManager(getApplicationContext());
        currentProjectNo = pm.getString("projectId");
        currentUserName = pm.getString("userId");

        po_url = pm.getString("SERVER_URL") + "/getPurchaseOrders?projectId='"+currentProjectNo+"'";

        cd = new ConnectionDetector(getApplicationContext());
        isInternetPresent = cd.isConnectingToInternet();

        text_vendor_invoice_no = (EditText) findViewById(R.id.text_vendor_invoice_no);
        text_vendor = (EditText) findViewById(R.id.text_vendor);
        date_created = (HelveticaRegular) findViewById(R.id.date_created);

        text_vendor.setEnabled(false);

        date_created.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        InvoiceCreate.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        spinner_po = (Spinner) findViewById(R.id.spinner_po);

        spinner_po.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                currentVendor = vendorIdArray[position];
                text_vendor.setText(currentVendor);

                if (position == 0)
                {
                    spinnerReceipt.setVisibility(View.GONE);

                } else {
                    final String currentPo = spinner_po.getSelectedItem().toString();
                    receiptUrl = pm.getString("SERVER_URL") + "/getPurchaseReceipts?projectId=\"" + currentProjectNo + "\"";

                    if (!isInternetPresent) {
                        // Internet connection is not present
                        // Ask user to connect to Internet
                        LinearLayout main_layout = (LinearLayout) findViewById(R.id.main_layout);
                        Crouton.cancelAllCroutons();
                        Crouton.makeText(InvoiceCreate.this, R.string.no_internet_error, Style.ALERT, main_layout).show();

                        pDialog = new ProgressDialog(InvoiceCreate.this);
                        pDialog.setMessage("Getting cache data");
                        pDialog.show();

                        Cache cache = AppController.getInstance().getRequestQueue().getCache();
                        Cache.Entry entry = cache.get(receiptUrl);
                        if (entry != null) {
                            //Cache data available.

                            try {
                                String data = new String(entry.data, "UTF-8");
                                Log.d("CACHE DATA", data);
                                JSONObject jsonObject = new JSONObject(data);
                                try {
                                    dataArray = jsonObject.getJSONArray("data");
                                    int j = 0;

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        dataObject = dataArray.getJSONObject(i);
                                        matchPurchaseOrderId = dataObject.getString("purchaseOrderId");

                                        if (matchPurchaseOrderId.equals(currentPo)) {
                                            j++;
                                        }
                                    }

                                    receiptArray = new String[j + 1];
                                    receiptArray[0] = "No Purchase Receipt";

                                    j = 0;

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        dataObject = dataArray.getJSONObject(i);
                                        matchPurchaseOrderId = dataObject.getString("purchaseOrderId");

                                        if (matchPurchaseOrderId.equals(currentPo)) {
                                            purchaseReceiptId = dataObject.getString("purchaseReceiptId");
                                            receiptArray[j + 1] = purchaseReceiptId;
                                            j++;
                                        }
                                    }

                                    if (j == 0) {
                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                                android.R.layout.simple_dropdown_item_1line, new String[]{"No Purchase Receipt"});
                                        spinnerReceipt.setAdapter(adapter);
                                    } else {
                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                                android.R.layout.simple_dropdown_item_1line, receiptArray);
                                        receiptArray[0] = "Select Purchase Receipt";

                                        spinnerReceipt.setAdapter(adapter);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
//                    Toast.makeText(getApplicationContext(), "Loading from cache.", Toast.LENGTH_SHORT).show();
                            } catch (UnsupportedEncodingException | JSONException e) {
                                e.printStackTrace();
                            }
                            if (pDialog != null)
                                pDialog.dismiss();
                        } else {
                            Toast.makeText(InvoiceCreate.this, "Offline Data Not available for Receipt", Toast.LENGTH_SHORT).show();
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                    android.R.layout.simple_dropdown_item_1line, new String[]{"Data not available"});
                            spinnerReceipt.setAdapter(adapter);
                            pDialog.dismiss();
                        }
                    }
                    else
                    {
                        // Cache data not exist.
                        prepareReceiptNos(currentPo, receiptUrl);
                    }
                    spinnerReceipt.setVisibility(View.VISIBLE);
                    currentVendor = vendorIdArray[position];
                    Log.d("current Vendor :", currentVendor);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (!isInternetPresent)
        {
            // Internet connection is not present
            // Ask user to connect to Internet
            LinearLayout main_layout = (LinearLayout) findViewById(R.id.main_layout);
            Crouton.cancelAllCroutons();
            Crouton.makeText(InvoiceCreate.this, R.string.no_internet_error, Style.ALERT, main_layout).show();

            pDialog = new ProgressDialog(InvoiceCreate.this);
            pDialog.setMessage("Getting cache data");
            pDialog.show();

            Cache cache = AppController.getInstance().getRequestQueue().getCache();
            Cache.Entry entry = cache.get(po_url);
            if (entry != null) {
                //Cache data available.
                try {
                    String data = new String(entry.data, "UTF-8");
                    Log.d("CACHE DATA", data);
                    JSONObject jsonObject = new JSONObject(data);
                    try {
                        dataArray = jsonObject.getJSONArray("data");
                        purchaseOrdersArray = new String[dataArray.length()+1];
                        vendorIdArray = new String[dataArray.length()+1];

                        purchaseOrdersArray[0]="Select Purchase Orders";
                        vendorIdArray[0]="Select PO";
                        for(int i=0; i<dataArray.length();i++)
                        {
                            dataObject = dataArray.getJSONObject(i);
                            poId = dataObject.getString("purchaseOrderId");
                            vendorId = dataObject.getString("vendorId");

                            purchaseOrdersArray[i+1]=poId;
                            vendorIdArray[i+1] = vendorId;
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                android.R.layout.simple_dropdown_item_1line,purchaseOrdersArray);
                        spinner_po.setAdapter(adapter);
                        pDialog.dismiss();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    Toast.makeText(getApplicationContext(), "Loading from cache.", Toast.LENGTH_SHORT).show();
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }
                if (pDialog != null)
                    pDialog.dismiss();
            }

            else
            {
                Toast.makeText(InvoiceCreate.this, "Offline Data Not available for PO", Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            }
        }

        else
        {
            // Cache data not exist.
            preparePurchaseOrderList();
        }



//        spinner_po.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                if (position == 0) {
//                    spinnerReceipt.setVisibility(View.GONE);
//                    text_vendor.setText(vendorIdArray[position]);
//
//                } else {
//                    pDialog = new ProgressDialog(InvoiceCreate.this);
//                    pDialog.setMessage("Getting Purchase Orders...");
//                    pDialog.setIndeterminate(false);
//                    pDialog.setCancelable(true);
//                    pDialog.show();
//
//                    final String currentPo = spinner_po.getSelectedItem().toString();
//
//                    class MyTask extends AsyncTask<Void, Void, Void> {
//                        @Override
//                        protected Void doInBackground(Void... params) {
//                            prepareReceiptNos(currentPo);
//                            return null;
//                        }
//                    }
//
//                    new MyTask().execute();
//                    spinnerReceipt.setVisibility(View.VISIBLE);
//                    text_vendor.setText(vendorIdArray[position]);
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });


        Button createBtn;
        createBtn = (Button) findViewById(R.id.createBtn);


        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (text_vendor_invoice_no.getText().toString().isEmpty()) {
                    text_vendor_invoice_no.setError("This field cannot be empty");
                } else if (date_created.getText().toString().isEmpty()) {
                    date_created.setError("Select Date");
                } else if (spinnerReceipt.getSelectedItem().toString().equals("Select Receipt")) {
                    Toast.makeText(InvoiceCreate.this, "Select Purchase Receipt", Toast.LENGTH_SHORT).show();
                } else if (spinnerReceipt.getSelectedItem().toString().equals("No Purchase Receipt")) {
                    Toast.makeText(InvoiceCreate.this, "No Purchase Receipts in this PO", Toast.LENGTH_SHORT).show();
                }
                else if (spinnerReceipt.getSelectedItem().toString().equals("Select Purchase Receipt"))
                {
                    Toast.makeText(InvoiceCreate.this, "Select Purchase Receipt", Toast.LENGTH_SHORT).show();
                }
                else if(spinnerReceipt.getSelectedItem().toString().equals("Data not available"))
                {
                    Toast.makeText(InvoiceCreate.this, "Data is not available for this Purchase Order", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    pDialog = new ProgressDialog(InvoiceCreate.this);
                    pDialog.setMessage("Sending Data ...");
                    pDialog.setIndeterminate(false);
                    pDialog.setCancelable(true);
                    pDialog.show();


                    saveItems();
                }

            }
        });
    }

    public void saveItems() {
                JSONObject object = new JSONObject();

                try
                {
                    Date tradeDate = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).parse(date_created.getText().toString());
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(tradeDate);
                    object.put("vendorInvoiceNo", text_vendor_invoice_no.getText().toString());
                    object.put("vendorId", text_vendor.getText().toString());
                    object.put("purchaseOrderNumber", spinnerReceipt.getSelectedItem().toString());
                    object.put("createdBy",currentUserName);
                    object.put("createdDate", formattedDate);
                    object.put("projectId", currentProjectNo);

                }
                catch (JSONException | ParseException e)
                {
                    e.printStackTrace();
                }

            RequestQueue requestQueue = Volley.newRequestQueue(InvoiceCreate.this);

                String url = InvoiceCreate.this.pm.getString("SERVER_URL") + "/postInvoice";

                JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, object,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    if(response.getString("msg").equals("success"))
                                    {
                                        Toast.makeText(InvoiceCreate.this, "Invoice Created. ID - " + response.getString("data"), Toast.LENGTH_SHORT).show();
                                        updateReceiptSelected(spinnerReceipt.getSelectedItem().toString());
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    pDialog.dismiss();
                                }
                                //response success message display
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e("Volley", "Error");
                                pDialog.dismiss();
                            }
                        }
                );
        if (!isInternetPresent)
        {
            pDialog.dismiss();
            // Internet connection is not present

            Communicator communicator = new Communicator();
            Log.d("object", object.toString());

            Boolean createInvoicePending = pm.getBoolean("createInvoicePending");

            if(createInvoicePending)
            {
                Toast.makeText(InvoiceCreate.this, "Already an Invoice creation is in progress. Please try after sometime.", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(InvoiceCreate.this, "Internet not currently available. Invoice will automatically get created on internet connection.", Toast.LENGTH_SHORT).show();

                pm.putString("objectInvoice", object.toString());
                pm.putString("urlInvoice", url);
                pm.putString("toastMessageInvoice", "Invoice Created");
                pm.putBoolean("createInvoicePending", true);

                updateReceiptSelected(spinnerReceipt.getSelectedItem().toString());
            }
        }
        else
        {
            requestQueue.add(jor);
        }
    }

    public void updateReceiptSelected(String prId)
    {
        JSONObject object = new JSONObject();

        try {
            object.put("isSelected", "1");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(InvoiceCreate.this);

        String url = InvoiceCreate.this.pm.getString("SERVER_URL") + "/putPurchaseReceiptSelected?purchaseReceiptId=\""+ prId + "\"";

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.PUT, url, object,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getString("msg").equals("success"))
                            {
                                pDialog.dismiss();
                                Intent intent = new Intent(InvoiceCreate.this, AllInvoices.class);
                                startActivity(intent);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            pDialog.dismiss();
                        }
                        //response success message display
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley", "Error");
                        pDialog.dismiss();
                    }
                }
        );
        if (!isInternetPresent)
        {
            // Internet connection is not present

            Communicator communicator = new Communicator();
            Log.d("object", object.toString());

            Boolean updateInvoicePrReceipt = pm.getBoolean("updateInvoicePrReceipt");

            if(updateInvoicePrReceipt)
            {
                Toast.makeText(InvoiceCreate.this, "Already an Invoice creation is in progress. Please try after sometime.", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(InvoiceCreate.this, "Internet not currently available. Invoice will automatically get created on internet connection.", Toast.LENGTH_SHORT).show();

                pm.putString("objectUpdateInvoicePrReceipt", object.toString());
                pm.putString("urlUpdateInvoicePrReceipt", url);
                pm.putString("toastMessageUpdateInvoicePrReceipt", "Invoice created for Purchase Receipt : " + prId);
                pm.putBoolean("updateInvoicePrReceipt", true);

                Intent intent = new Intent(InvoiceCreate.this, AllInvoices.class);
                startActivity(intent);
            }
        }
        else
        {
            requestQueue.add(jor);
        }
    }

    private void preparePurchaseOrderList() {
        // TODO Auto-generated method stub

        pDialog = new ProgressDialog(InvoiceCreate.this);
        pDialog.setMessage("Getting server data");
        pDialog.show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, po_url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try
                        {

                            String type = response.getString("type");

                            if(type.equals("ERROR"))
                            {
                                Toast.makeText(InvoiceCreate.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }

                            if(type.equals("INFO"))
                            {
                                dataArray = response.getJSONArray("data");
                                purchaseOrdersArray = new String[dataArray.length()+1];
                                vendorIdArray = new String[dataArray.length()+1];

                                purchaseOrdersArray[0]="Select Purchase Orders";
                                vendorIdArray[0]="Select PO";
                                for(int i=0; i<dataArray.length();i++)
                                {
                                    dataObject = dataArray.getJSONObject(i);
                                    poId = dataObject.getString("purchaseOrderId");
                                    vendorId = dataObject.getString("vendorId");

                                    purchaseOrdersArray[i+1]=poId;
                                    vendorIdArray[i+1] = vendorId;
                                }
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                        android.R.layout.simple_dropdown_item_1line,purchaseOrdersArray);
                                spinner_po.setAdapter(adapter);

                                pDialog.dismiss();

                            }
                            else
                            {
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                        android.R.layout.simple_dropdown_item_1line,new String[] {"No PO Found"});
                                spinner_po.setAdapter(adapter);

                                pDialog.dismiss();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
//                        setData(response,false);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            }
        });
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjectRequest);
        if(pDialog!=null)
            pDialog.dismiss();
    }

    public void prepareReceiptNos(final String poNumber, String receiptUrl)
    {
        RequestQueue requestQueue = Volley.newRequestQueue(this);


        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, receiptUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try{

                            String type = response.getString("type");
                            String msg = response.getString("msg");

                            if(type.equals("ERROR"))
                            {
                                Toast.makeText(InvoiceCreate.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }

                            if(type.equals("INFO"))
                            {
                                Log.d("current PO : ", poNumber);

                                Log.d("response full : ", response.toString());

                                dataArray = response.getJSONArray("data");

                                int j=0;

                                for(int i=0; i<dataArray.length();i++)
                                {
                                    dataObject = dataArray.getJSONObject(i);
                                    matchPurchaseOrderId = dataObject.getString("purchaseOrderId");

                                    if(matchPurchaseOrderId.equals(poNumber))
                                    {
                                        j++;
                                    }
                                }

                                receiptArray = new String[j+1];
                                receiptArray[0]="No Purchase Receipt";

                                j=0;

                                for(int i=0; i<dataArray.length();i++)
                                {
                                    dataObject = dataArray.getJSONObject(i);
                                    matchPurchaseOrderId = dataObject.getString("purchaseOrderId");

                                    if(matchPurchaseOrderId.equals(poNumber))
                                    {
                                        purchaseReceiptId = dataObject.getString("purchaseReceiptId");
                                        receiptArray[j+1] = purchaseReceiptId;
                                        j++;
                                    }
                                }

                                if(j==0)
                                {
                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                            android.R.layout.simple_dropdown_item_1line,new String[] {"No Purchase Receipt"});
                                    spinnerReceipt.setAdapter(adapter);
                                }
                                else
                                {
                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(InvoiceCreate.this,
                                            android.R.layout.simple_dropdown_item_1line,receiptArray);
                                    receiptArray[0]="Select Purchase Receipt";

                                    spinnerReceipt.setAdapter(adapter);
                                }
                                pDialog.dismiss();
                            }
                            if(msg.equals("No data"))
                            {
                                Toast.makeText(InvoiceCreate.this, "No Purchase Receipt Found.", Toast.LENGTH_LONG).show();
                                pDialog.dismiss();
                                finish();
                            }

                        }catch(JSONException e){e.printStackTrace();
                            pDialog.dismiss();}
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pDialog.dismiss();
                        Log.e("Volley","Error");

                    }
                }
        );
        requestQueue.add(jor);
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {

        String[] MONTHS = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
        String date = dayOfMonth+"-"+(MONTHS[monthOfYear])+"-"+year;

        date_created.setText(date);
    }
}
