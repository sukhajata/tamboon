package omise.sukhajata.com.timomise;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.squareup.picasso.Picasso;

import omise.sukhajata.com.timomise.interfaces.DownloadCallback;
import omise.sukhajata.com.timomise.model.Charity;
import omise.sukhajata.com.timomise.utility.ApiManager;

import java.util.List;

/**
 * An activity representing a list of Charities.
 */
public class CharityListActivity extends AppCompatActivity implements
        DownloadCallback,
        ProviderInstaller.ProviderInstallListener{

    private static final int DONATION = 100;
    private static final int RESULT = 200;
    private static final int ERROR_DIALOG_REQUEST_CODE = 1;

    private boolean mRetryProviderInstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charity_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        //update encryption for old Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
           ProviderInstaller.installIfNeededAsync(this, this);
        } else {
            //load charity data
           loadCharities();
        }

    }


    private void loadCharities() {
        if (isConnected()) {
            ApiManager.getInstance(this).getCharities(this);
        } else {
            TextView txtError = findViewById(R.id.error_message);
            txtError.setText("Not connected to the internet.");
        }
    }
    /**
     * This method is only called if the provider is successfully updated
     * (or is already up-to-date).
     */
    @Override
    public void onProviderInstalled() {
        // Provider is up-to-date, app can make secure network calls.
        ApiManager.getInstance(this).getCharities(this);
    }

    /**
     * This method is called if updating fails; the error code indicates
     * whether the error is recoverable.
     */
    @Override
    public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        if (availability.isUserResolvableError(errorCode)) {
            // Recoverable error. Show a dialog prompting the user to
            // install/update/enable Google Play services.
            availability.showErrorDialogFragment(
                    this,
                    errorCode,
                    ERROR_DIALOG_REQUEST_CODE,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            // The user chose not to take the recovery action
                            onProviderInstallerNotAvailable();
                        }
                    });
        } else {
            // Google Play services is not available.
            onProviderInstallerNotAvailable();
        }
    }

    /**
     * On resume, check to see if we flagged that we need to reinstall the
     * provider.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mRetryProviderInstall) {
            // We can now safely retry installation.
            ProviderInstaller.installIfNeededAsync(this, this);
        }
        mRetryProviderInstall = false;
    }

    private void onProviderInstallerNotAvailable() {
        // This is reached if the provider cannot be updated for some reason.
        // App should consider all HTTP communication to be vulnerable, and take
        // appropriate action.
    }

    @Override
    public void onDownloadCompleted(Object result) {
        if (result instanceof List) {
            RecyclerView recyclerView = findViewById(R.id.charity_list);
            assert recyclerView != null;
            recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this, (List<Charity>)result));
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

        showError(message + ": " + msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DONATION:
                if (resultCode == RESULT_OK) {
                    String result = data.getData().toString();
                    Intent intent = new Intent(this, ResultActivity.class);
                    intent.putExtra(ResultActivity.ARG_RESULT, result);
                    startActivity(intent);
                }
                break;
            case ERROR_DIALOG_REQUEST_CODE:
                // Adding a fragment via GoogleApiAvailability.showErrorDialogFragment
                // before the instance state is restored throws an error. So instead,
                // set a flag here, which will cause the fragment to delay until
                // onPostResume.
                mRetryProviderInstall = true;
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showError(String msg) {
        TextView txtError = findViewById(R.id.error_message);
        txtError.setText(msg);
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final CharityListActivity mParentActivity;
        private final List<Charity> mValues;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Charity item = (Charity) view.getTag();

                Intent intent = new Intent(mParentActivity, DonationActivity.class);
                intent.putExtra(DonationActivity.ARG_CHARITY_NAME, item.name);
                mParentActivity.startActivityForResult(intent, DONATION);

            }
        };

        SimpleItemRecyclerViewAdapter(CharityListActivity parent,
                                      List<Charity> items) {
            mValues = items;
            mParentActivity = parent;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.charity_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mNameView.setText(mValues.get(position).name);
            Picasso.get().load(mValues.get(position).logo_url).into(holder.mImageView);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mNameView;
            final ImageView mImageView;

            ViewHolder(View view) {
                super(view);
                mNameView = (TextView) view.findViewById(R.id.charity_name);
                mImageView = (ImageView) view.findViewById(R.id.charity_image);
            }
        }
    }
}
