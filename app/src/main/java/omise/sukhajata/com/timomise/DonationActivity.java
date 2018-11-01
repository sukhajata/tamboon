package omise.sukhajata.com.timomise;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.Calendar;

import co.omise.android.CardNumber;
import co.omise.android.Client;
import co.omise.android.TokenRequest;
import co.omise.android.TokenRequestListener;
import co.omise.android.models.Token;
import co.omise.android.ui.CreditCardEditText;
import omise.sukhajata.com.timomise.interfaces.DownloadCallback;
import omise.sukhajata.com.timomise.utility.ApiManager;

public class DonationActivity extends AppCompatActivity implements DownloadCallback {

    public static final String ARG_CHARITY_NAME = "charity_name";

    private static final String OMISE_PKEY = "pkey_test_5dr3nozb08cmr522ojz";

    private int mMonth, mYear;

    private boolean mNameValid, mNumberValid, mDateValid, mCvvValid, mAmountValid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        String charityName = getIntent().getExtras().getString(ARG_CHARITY_NAME);
        getSupportActionBar().setTitle("Donate to " + charityName);

        //populate expiry date spinners
        Spinner month = findViewById(R.id.card_expiry_month);
        month.setAdapter(new MonthSpinnerAdapter());
        month.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                checkDateValid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                checkDateValid();
            }
        });

        Spinner year = findViewById(R.id.card_expiry_year);
        year.setAdapter(new YearSpinnerAdapter());
        year.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long l) {
                checkDateValid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                checkDateValid();
            }
        });

        //set up validation
        EditText name = findViewById(R.id.card_name);
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 3) {
                    mNameValid = true;
                    checkValidity();
                } else {
                    mNameValid = false;
                    checkValidity();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        CreditCardEditText txtNumber = findViewById(R.id.card_number);
        txtNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String number = charSequence.toString().replaceAll("\\s+", "");
                if (number.length() == 16 && CardNumber.luhn(charSequence.toString())) {
                    mNumberValid = true;
                    checkValidity();
                } else {
                    mNumberValid = false;
                    checkValidity();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        EditText txtCvv = findViewById(R.id.card_cvv);
        txtCvv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 3) {
                    mCvvValid = true;
                    checkValidity();
                } else {
                    mCvvValid = false;
                    checkValidity();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        EditText txtAmount = findViewById(R.id.card_amount);
        txtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence != null && Integer.parseInt(charSequence.toString()) > 20) {
                    mAmountValid = true;
                    checkValidity();
                } else {
                    mAmountValid = false;
                    checkValidity();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //capture click of submit button
        View submitButton = findViewById(R.id.card_submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processCardData();
            }
        });
    }


    private void checkDateValid() {
        Spinner spnMonth = findViewById(R.id.card_expiry_month);
        int month = (int)spnMonth.getSelectedItem();

        Spinner spnYear = findViewById(R.id.card_expiry_year);
        int year = (int)spnYear.getSelectedItem();

        Calendar c = Calendar.getInstance();
        c.set(year, month, 1, 0, 0);

        //compare date to today
        int result = c.compareTo(Calendar.getInstance());
        if (result > 0) {
            mDateValid = true;
        } else {
            mDateValid = false;
        }
    }

    private void checkValidity() {
        checkDateValid();
        Button btnSubmit = findViewById(R.id.card_submit);
        if (mNameValid && mNumberValid && mDateValid && mCvvValid && mAmountValid) {
            btnSubmit.setEnabled(true);
        } else {

            btnSubmit.setEnabled(false);
        }
    }

    private void processCardData() {
        if (isConnected()) {
            View card_content = findViewById(R.id.card_content);
            card_content.setVisibility(View.GONE);

            View progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);

            EditText txtName = findViewById(R.id.card_name);
            String name = txtName.getText().toString();

            CreditCardEditText txtCardNumber = findViewById(R.id.card_number);
            String number = txtCardNumber.getText().toString();

            Spinner spnMonth = findViewById(R.id.card_expiry_month);
            int month = (int) spnMonth.getSelectedItem();

            Spinner spnYear = findViewById(R.id.card_expiry_year);
            int year = (int) spnYear.getSelectedItem();

            EditText txtCvv = findViewById(R.id.card_cvv);
            String cvv = txtCvv.getText().toString();

            EditText txtAmount = findViewById(R.id.card_amount);
            final int amount = Integer.parseInt(txtAmount.getText().toString()) * 100;

            try {
                Client client = new Client(OMISE_PKEY);
                TokenRequest request = new TokenRequest();
                request.number = number;
                request.name = name;
                request.expirationMonth = month;
                request.expirationYear = year;
                request.securityCode = cvv;

                final DonationActivity context = this;

                client.send(request, new TokenRequestListener() {
                    @Override
                    public void onTokenRequestSucceed(TokenRequest request, Token token) {
                        ApiManager.getInstance(context).makeDonation(token.id, request.name, amount, context);
                    }

                    @Override
                    public void onTokenRequestFailed(TokenRequest request, Throwable throwable) {
                        onCompleted("Token request failed " + throwable.getMessage());
                    }
                });

            } catch (GeneralSecurityException ex) {
                onCompleted(ex.getMessage());
            }
        } else {
            onCompleted("Not connected to the internet.");
        }

    }

    @Override
    public void onDownloadCompleted(Object result) {

        if (result instanceof JSONObject) {
          JSONObject json = (JSONObject) result;
          try {
              String message = (String)json.get("message");
              onCompleted(message);
          } catch (JSONException ex) {
              onCompleted(ex.getMessage());
          }
        } else {
            onCompleted(result.toString());
        }
    }

    @Override
    public boolean isConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnectedOrConnecting();
        return connected;
    }


    @Override
    public void onDownloadError(int code, String msg) {
        String message = "";
        switch (code) {
            case DownloadCallback.PARSING_ERROR:
                message = "Error parsing data";
                break;
            case DownloadCallback.ERROR:
                message = "Error";
                break;
        }
        onCompleted(message + ": " + msg);
    }


    private void onCompleted(String msg) {
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        Intent intent = new Intent();
        intent.setData(Uri.parse(msg));
        setResult(RESULT_OK, intent);
        finish();
    }


}
