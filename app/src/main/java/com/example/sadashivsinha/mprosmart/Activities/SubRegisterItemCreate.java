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
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class SubRegisterItemCreate extends AppCompatActivity {

    ProgressDialog pDialog, pDialog1;
    EditText text_item_title;
    String currentSubRegId;
    BetterSpinner spinner_status, spinner_type, spinner_contract;
    String[] submittalTypeArray, submittalTypeIdArray;
    String submittalType, submittalTypeId, currentSubmittalTypeId;
    JSONArray dataArray;
    JSONObject dataObject;
    ConnectionDetector cd;
    public static final String TAG = SubRegisterItemCreate.class.getSimpleName();
    Boolean isInternetPresent = false;
    PreferenceManager pm;
    String url;

    Button attachBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_register_item_create);

        Button createBtn;

        attachBtn = (Button) findViewById(R.id.attachBtn);

        pm = new PreferenceManager(this);
        currentSubRegId =pm.getString("submittalRegistersId");

        text_item_title = (EditText) findViewById(R.id.text_item_title);

        spinner_contract = (BetterSpinner) findViewById(R.id.spinner_contract);

        spinner_type = (BetterSpinner) findViewById(R.id.spinner_type);

        spinner_status = (BetterSpinner) findViewById(R.id.spinner_status);

        ArrayAdapter<String> adapterUom = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[] {"Active", "Inactive", "Pending"});

        spinner_status.setAdapter(adapterUom);

        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SubRegisterItemCreate.this, AttachmentActivity.class);
                intent.putExtra("class", "SubmittalRegLine");
                startActivity(intent);
            }
        });

        ArrayAdapter<String> adapterContractId = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[] {"CON001"});

        spinner_contract.setAdapter(adapterContractId);

        createBtn = (Button) findViewById(R.id.createBtn);


        cd = new ConnectionDetector(getApplicationContext());
        isInternetPresent = cd.isConnectingToInternet();


        spinner_type.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentSubmittalTypeId = submittalTypeIdArray[position];
            }
        });

        url = pm.getString("SERVER_URL") + "/getSubmittalType";


        if (!isInternetPresent) {
            // Internet connection is not present
            // Ask user to connect to Internet
            LinearLayout main_layout = (LinearLayout) findViewById(R.id.main_layout);
            Crouton.cancelAllCroutons();
            Crouton.makeText(SubRegisterItemCreate.this, R.string.no_internet_error, Style.ALERT, main_layout).show();

            pDialog = new ProgressDialog(SubRegisterItemCreate.this);
            pDialog.setMessage("Getting cache data");
            pDialog.show();

            Cache cache = AppController.getInstance().getRequestQueue().getCache();
            Cache.Entry entry = cache.get(url);
            if (entry != null) {
                //Cache data available.
                try {
                    String data = new String(entry.data, "UTF-8");
                    Log.d("CACHE DATA", data);
                    JSONObject jsonObject = new JSONObject(data);

                    try {
                        dataArray = jsonObject.getJSONArray("data");
                        submittalTypeArray = new String[dataArray.length()];
                        submittalTypeIdArray = new String[dataArray.length()];
                        for(int i=0; i<dataArray.length();i++)
                        {
                            dataObject = dataArray.getJSONObject(i);
                            submittalType = dataObject.getString("submittalType");
                            submittalTypeId = dataObject.getString("submittalTypeId");

                            submittalTypeArray[i]=submittalType;
                            submittalTypeIdArray[i]=submittalTypeId;
                        }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(SubRegisterItemCreate.this,
                            android.R.layout.simple_dropdown_item_1line,submittalTypeArray);
                    spinner_type.setAdapter(adapter);
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
                Toast.makeText(SubRegisterItemCreate.this, "Offline Data Not available for this Submittal Register", Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            }
        }

        else
        {
            // Cache data not exist.
            callJsonArrayRequest();
        }


        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveItem();
            }
        });
    }
    public void saveItem()
    {
        JSONObject object = new JSONObject();

        try
        {
            object.put("submittaltittle",text_item_title.getText().toString());
            object.put("submittalTypeId",currentSubmittalTypeId);
            object.put("submittalRegistersId",currentSubRegId);
            Log.d("CURRENT SUB REGISTER ID",currentSubRegId);
            if(spinner_status.getText().toString().equals("Active"))
            {
                object.put("submittalStatusId","1");
            }
            else
            {
                object.put("submittalStatusId","2");
            }
            object.put("contractId","CON001");

            if(pm.getString("className").equals("SubmittalRegister"))
            {
                object.put("lineNo", pm.getString("totalImageUrlSize"));
            }
            else
            {
                object.put("lineNo", "0");
            }

            if(pm.getString("className").equals("SubmittalRegLine"))
            {
                object.put("totalAttachments", pm.getString("totalImageUrlSize"));
            }

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(SubRegisterItemCreate.this);

        String url = SubRegisterItemCreate.this.pm.getString("SERVER_URL") + "/postSubmittalregisterLineItems";

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, object,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d("SubReg create", response.toString());

                            if(response.getString("msg").equals("success"))
                            {
                                String totalImageUrls = pm.getString("totalImageUrls");
                                if(pm.getString("className").equals("SubmittalRegLine") && isInternetPresent
                                        && !totalImageUrls.isEmpty())
                                {
                                    uploadImage(response.getString("data"), totalImageUrls);
                                }

                                else
                                {
                                    if (pDialog != null)
                                        pDialog.dismiss();

                                    Toast.makeText(SubRegisterItemCreate.this, "Submittal Register Line Item Created. ID - " + response.getString("data"), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(SubRegisterItemCreate.this, SubmittalRegisterActivity.class);
                                    startActivity(intent);
                                }
                            }
                            else
                            {
                                Toast.makeText(SubRegisterItemCreate.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }

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
                    }
                }
        );
        cd = new ConnectionDetector(getApplicationContext());
        isInternetPresent = cd.isConnectingToInternet();

        if (!isInternetPresent)
        {
            // Internet connection is not present

            Communicator communicator = new Communicator();
            Log.d("object", object.toString());

            Boolean createSubmittalRegItemPending = pm.getBoolean("createSubmittalRegItemPending");

            if(createSubmittalRegItemPending)
            {
                Toast.makeText(SubRegisterItemCreate.this, "Already a Submittal Register Item creation is in progress. Please try after sometime.", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(SubRegisterItemCreate.this, "Internet not currently available. Submittal Register Item will automatically get created on internet connection.", Toast.LENGTH_SHORT).show();

                pm.putString("objectSubmittalRegItem", object.toString());
                pm.putString("urlSubmittalRegItem", url);
                pm.putString("toastMessageSubmittalRegItem", "Submittal Register Line Item Created");
                pm.putBoolean("createSubmittalRegItemPending", true);

                Intent intent = new Intent(SubRegisterItemCreate.this, SubmittalRegisterActivity.class);
                startActivity(intent);
            }
        }
        else
        {
            requestQueue.add(jor);
        }
    }

    private void callJsonArrayRequest() {
        // TODO Auto-generated method stub

        pDialog = new ProgressDialog(SubRegisterItemCreate.this);
        pDialog.setMessage("Getting server data");
        pDialog.show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
//                            dataObject = response.getJSONObject(0);
                            dataArray = response.getJSONArray("data");

                            submittalTypeArray = new String[dataArray.length()];
                            submittalTypeIdArray = new String[dataArray.length()];
                            for(int i=0; i<dataArray.length();i++)
                            {
                                dataObject = dataArray.getJSONObject(i);
                                submittalType = dataObject.getString("submittalType");
                                submittalTypeId = dataObject.getString("submittalTypeId");

                                submittalTypeArray[i]=submittalType;
                                submittalTypeIdArray[i]=submittalTypeId;
                            }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SubRegisterItemCreate.this,
                                android.R.layout.simple_dropdown_item_1line, submittalTypeArray);
                        spinner_type.setAdapter(adapter);

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
            }
        });
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjectRequest);
        if(pDialog!=null)
            pDialog.dismiss();
    }

    public void uploadImage(final String id, String totalUrls) {

        final List<String> imageList = Arrays.asList(totalUrls.split(","));
        String seperateImageUrl;


        for (int i = 0; i < imageList.size(); i++) {

            final int count = i;
            JSONObject object = new JSONObject();

            try {
                seperateImageUrl = imageList.get(i);

                object.put("submittalregisterLineItemsId", id);
                object.put("url", seperateImageUrl);

                Log.d("JSON OBJ SENT", object.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }


            RequestQueue requestQueue = Volley.newRequestQueue(SubRegisterItemCreate.this);

            String url = SubRegisterItemCreate.this.pm.getString("SERVER_URL") + "/postSubmittalregisterLineItemFiles";

            JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, object,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {

                                Log.d("RESPONSE SERVER : ", response.toString());

                                if (response.getString("msg").equals("success")) {

                                    if (count == imageList.size() - 1) {
                                        Toast.makeText(SubRegisterItemCreate.this, "Submittal Register Line created ID - " + id, Toast.LENGTH_SHORT).show();

                                        if (pDialog != null)
                                            pDialog.dismiss();
                                        Intent intent = new Intent(SubRegisterItemCreate.this, SubmittalRegisterActivity.class);
                                        startActivity(intent);
                                    }
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //response success message display
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Volley", "Error");
                            Toast.makeText(SubRegisterItemCreate.this, error.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            requestQueue.add(jor);
        }
    }
}
