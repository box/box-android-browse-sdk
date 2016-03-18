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
 */
public class BoxResponseIntent<E extends BoxObject> extends Intent {

    private final E mResult;
    private final boolean mIsSuccess;
    private final BoxRequest mRequest;
    private final Exception mException;

    public BoxResponseIntent(BoxResponse<E> response) {
        mIsSuccess = response.isSuccess();
        mResult = response.getResult();
        mRequest = response.getRequest();
        mException = response.getException();
        if (mRequest != null) {
            setAction(mRequest.getClass().getName());
        }
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    public BoxRequest getRequest() {
        return mRequest;
    }

    public E getResult() {
        return mResult;
    }

    public Exception getException() {
        return mException;
    }

    @Override
    public int describeContents() {
        return super.describeContents();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
    }

    public static final Parcelable.Creator<BoxResponseIntent> CREATOR = new Creator<BoxResponseIntent>() {
        @Override
        public BoxResponseIntent createFromParcel(Parcel source) {
            return null;
        }

        @Override
        public BoxResponseIntent[] newArray(int size) {
            return new BoxResponseIntent[0];
        }
    };
}


