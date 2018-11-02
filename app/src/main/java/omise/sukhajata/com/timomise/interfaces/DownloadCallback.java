package omise.sukhajata.com.timomise.interfaces;

import android.net.NetworkInfo;

public interface DownloadCallback<T> {

    int PARSING_ERROR = -1;
    int ERROR = -2;

    void onDownloadCompleted(T result);

    /**
     * Get the device's active network status
     */
    boolean isConnected();


    void onDownloadError(int code, String msg);
}