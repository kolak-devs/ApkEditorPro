package com.termux.terminal.app.views;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.termux.terminal.databinding.LayoutProgressSheetBinding;

public class ProgressSheet extends BaseBottomSheetFragment {
    private LayoutProgressSheetBinding binding;
    private String message = "";
    private String subMessage = "";
    private boolean subMessageEnabled = false;
    private boolean welcomeTextEnabled = false;

    @Override
    protected void bind(@NonNull LinearLayout container) {
        binding = LayoutProgressSheetBinding.inflate(LayoutInflater.from(getContext()));
        container.addView(binding.getRoot());
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.message.setText(message);
        if (subMessageEnabled) {
            binding.subMessage.setText(subMessage);
            binding.subMessage.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams p =
                    (RelativeLayout.LayoutParams) binding.message.getLayoutParams();
            try {
                p.removeRule(RelativeLayout.CENTER_VERTICAL);
            } catch (Throwable th) {
                //LOG.error("Unable to remove center_vertical rule.", th);
            }
            binding.message.setLayoutParams(p);
        } else {
            binding.subMessage.setVisibility(View.GONE);
            RelativeLayout.LayoutParams p =
                    (RelativeLayout.LayoutParams) binding.message.getLayoutParams();
            try {
                p.addRule(RelativeLayout.CENTER_VERTICAL);
            } catch (Throwable th) {
                //LOG.error("Unable to remove center_vertical rule.", th);
            }
            binding.message.setLayoutParams(p);
        }

        if (!welcomeTextEnabled) {
            binding.welcomeText.setVisibility(View.GONE);
        }
    }

    @Override
    protected boolean shouldHideTitle() {
        return true;
    }

    public ProgressSheet setWelcomeTextEnabled(boolean enabled) {
        this.welcomeTextEnabled = enabled;
        return this;
    }

    public ProgressSheet setSubMessageEnabled(boolean enabled) {
        this.subMessageEnabled = enabled;
        return this;
    }

    public ProgressSheet setSubMessage(String msg) {
        this.subMessage = msg;
        if (isShowing()) {
            binding.subMessage.setText(msg);
        }
        return this;
    }

    public ProgressSheet setMessage(String message) {
        this.message = message;
        if (isShowing()) {
            binding.message.setText(message);
        }

        return this;
    }

    public ProgressSheet setProgressDrawable(Drawable drawable) {
        if (isShowing()) {
            binding.progress.setIndeterminateDrawable(drawable);
        }

        return this;
    }

    @Override
    public void dismiss() {
        if (isShowing()) super.dismiss();
    }
}