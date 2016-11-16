package com.box.androidsdk.browse.service;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.box.androidsdk.content.models.BoxObject;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.requests.BoxResponse;

/**
 * Intent meant to be used as a way to pass response messages from the service to the views. This
 * will hold all related information to a BoxResponse
 *
 * @param <E> the type parameter
 */
public class BoxResponseIntent<E extends BoxObject> extends Intent {
    private final BoxResponse<E> mResponse;

    /**
     * Instantiates a new Box response intent.
     *
     * @param response the response
     */
    public BoxResponseIntent(BoxResponse<E> response) {
        mResponse = response;
        if (mResponse.getRequest() != null) {
            setAction(mResponse.getRequest().getClass().getName());
        }
    }

    /**
     * Is success boolean. returns truw if the request was successful
     *
     * @return the boolean
     */
    public boolean isSuccess() {
        return mResponse.isSuccess();
    }

    /**
     * Gets the request associated with this response.
     *
     * @return the request
     */
    public BoxRequest getRequest() {
        return mResponse.getRequest();
    }

    /**
     * Gets the result from this response.
     *
     * @return the result
     */
    public E getResult() {
        return mResponse.getResult();
    }

    /**
     * Gets the exception from this response
     *
     * @return the exception
     */
    public Exception getException() {
        return mResponse.getException();
    }

    /**
     * Gets the response that we received from box server
     *
     * @return the response
     */
    public BoxResponse<E> getResponse() {
        return mResponse;
    }

    @Override
    public int describeContents() {
        return super.describeContents();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeSerializable(mResponse);
    }

    public static final Parcelable.Creator<BoxResponseIntent> CREATOR = new Creator<BoxResponseIntent>() {
        @Override
        public BoxResponseIntent createFromParcel(Parcel source) {
            return new BoxResponseIntent(source);
        }

        @Override
        public BoxResponseIntent[] newArray(int size) {
            return new BoxResponseIntent[size];
        }
    };

    private BoxResponseIntent(Parcel in) {
        readFromParcel(in);
        mResponse = (BoxResponse) in.readSerializable();
    }
}


