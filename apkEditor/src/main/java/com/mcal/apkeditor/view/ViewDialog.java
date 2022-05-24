package com.mcal.apkeditor.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;

public class ViewDialog extends Dialog {
    private final Context mContext;
    private FrameLayout mContentFrame;
    private TextView mCaption;
    private Button mPositive, mNegative, mNeutral;
    private View.OnClickListener positiveSet = null;
    private View.OnClickListener negativeSet = null;
    private View.OnClickListener neutralSet = null;

    public ViewDialog(@NonNull Context context) {
        super(context);
        mContext = context;
        initContentView();
    }

    private void initContentView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_view, null);
        mCaption = view.findViewById(R.id.content_caption);
        mContentFrame = view.findViewById(R.id.content_frame);
        mPositive = view.findViewById(R.id.positive);
        mNegative = view.findViewById(R.id.negative);
        mNeutral = view.findViewById(R.id.neutral);
        setContentView(view);
    }

    @Override
    public void setTitle(CharSequence title) {
        mCaption.setText(title);
    }

    public void setTitleTextSize(float size) {
        mCaption.setTextSize(size);
    }

    public void setView(View view) {
        mContentFrame.addView(view);
    }

    public void setNeutral(CharSequence text, View.OnClickListener listener) {
        mNeutral.setText(text);
        mNeutral.setOnClickListener(listener);
        neutralSet = listener;
    }

    public void setPositive(CharSequence text, View.OnClickListener listener) {
        mPositive.setText(text);
        mPositive.setOnClickListener(listener);
        positiveSet = listener;
    }

    public void setNegative(CharSequence text, View.OnClickListener listener) {
        mNegative.setText(text);
        mNegative.setOnClickListener(listener);
        negativeSet = listener;
    }

    public void setNeutral(int resId, View.OnClickListener listener) {
        mNeutral.setText(resId);
        mNeutral.setOnClickListener(listener);
        neutralSet = listener;
    }

    public void setPositive(int resId, View.OnClickListener listener) {
        mPositive.setText(resId);
        mPositive.setOnClickListener(listener);
        positiveSet = listener;
    }

    public void setNegative(int resId, View.OnClickListener listener) {
        mNegative.setText(resId);
        mNegative.setOnClickListener(listener);
        negativeSet = listener;
    }

    @Override
    public void show() {
        super.show();
        mNeutral.setVisibility(mNeutral.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
        mNegative.setVisibility(mNegative.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
        mPositive.setVisibility(mPositive.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
        if (mNeutral.getVisibility() == View.VISIBLE && neutralSet == null) {
            mNeutral.setOnClickListener(view -> dismiss());
        }
        if (mPositive.getVisibility() == View.VISIBLE && positiveSet == null) {
            mPositive.setOnClickListener(view -> dismiss());
        }
        if (mNegative.getVisibility() == View.VISIBLE && negativeSet == null) {
            mNegative.setOnClickListener(view -> dismiss());
        }
    }

    @Override
    public void dismiss() {
        /*if(mContentFrame != null) {
            mContentFrame = null;
        }
        if(mCaption != null) {
            mCaption = null;
        }
        if(mPositive != null) {
            mPositive = null;
        }
        if(mNegative != null) {
            mNegative = null;
        }
        if(mNeutral != null) {
            mNeutral = null;
        }
        super.dismiss();*/
        super.dismiss();
    }
}