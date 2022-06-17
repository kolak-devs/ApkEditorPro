package jackpal.androidterm.shortcuts;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import jackpal.androidterm.R;

public class ColorValue
        implements CompoundButton.OnCheckedChangeListener {
    private final Context context;
    private final int[] color = {0xFF, 0, 0, 0};
    private final boolean[] locks = {false, false, false, false};
    private final ImageView imgview;
    private final String[] result;
    private EditText value;
    private boolean started = false;
    private boolean barLock = false;
    private String imgtext = "";

    public ColorValue(Context context, final ImageView imgview, @NonNull final String[] result) {
        this.context = context;
        this.imgtext = result[0];
        this.imgview = imgview;
        this.result = result;
        colorValue();
    }

    public void colorValue() {
        final int arraySizes = 4;
        AlertDialog dialog1 = new MaterialAlertDialogBuilder(context).create();
        LinearLayout lv = new LinearLayout(context);
        lv.setOrientation(LinearLayout.VERTICAL);
        String[] lab = {
                context.getString(R.string.colorvalue_letter_alpha) + " " //"α "
                , context.getString(R.string.colorvalue_letter_red) + " " //"R "
                , context.getString(R.string.colorvalue_letter_green) + " " //"G "
                , context.getString(R.string.colorvalue_letter_blue) + " " //"B "
        };
        int[] clr = {0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        for (int i = 0, n = (Integer) imgview.getTag(); i < arraySizes; i++)
            color[i] = (n >> (24 - i * 8)) & 0xFF;
        TextView lt = new TextView(context);
        lt.setText(context.getString(R.string.colorvalue_label_lock_button_column));//"LOCK");
        lt.setPadding(lt.getPaddingLeft(), lt.getPaddingTop(), 5, lt.getPaddingBottom());
        lt.setGravity(Gravity.RIGHT);
        value = new EditText(context);
        value.setText(imgtext);
        value.setSingleLine(false);
        value.setGravity(Gravity.CENTER);
        value.setTextColor((Integer) imgview.getTag());
        value.setBackgroundColor((0xFF << 24) | 0x007799);
        LinearLayout vh = new LinearLayout(context);
        vh.setOrientation(LinearLayout.HORIZONTAL);
        vh.setGravity(Gravity.CENTER_HORIZONTAL);
        vh.addView(value);
        value.setHint(context.getString(R.string.colorvalue_icon_text_entry_hint));//"Enter icon text");
        lv.addView(vh);
        lv.addView(lt);
        final SeekBar[] sb = new SeekBar[arraySizes + 1];
        final CheckBox[] lk = new CheckBox[arraySizes];
        final TextView[] hexWindow = new TextView[arraySizes];
        for (int i = 0; i < arraySizes; i++) {
            LinearLayout lh = new LinearLayout(context);
            lh.setGravity(Gravity.CENTER_VERTICAL);
            final TextView tv = new TextView(context);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setText(lab[i]);
            tv.setTextColor(clr[i]);
            sb[i] = new SeekBar(context);
            sb[i].setMax(0xFF);
            sb[i].setProgress(color[i]);
            sb[i].setSecondaryProgress(color[i]);
            sb[i].setTag(i);
            sb[i].setBackgroundColor(0xFF << 24 | (color[i] << (24 - i * 8)));
            int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
            sb[i].setLayoutParams(new LinearLayout.LayoutParams(WC, WC, 1));
            sb[i].setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            doProgressChanged(seekBar, progress, fromUser);
                        }

                        private void doProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser && started) {
                                int me = (Integer) seekBar.getTag();
                                int k = (color[0] << 24) | (color[1] << 16) | (color[2] << 8) | color[3];
                                value.setTextColor(k);
                                int start, end;
                                if (barLock && locks[me]) {
                                    start = 0;
                                    end = arraySizes - 1;
                                } else start = end = (Integer) seekBar.getTag();
                                for (int i = start; i <= end; i++) {
                                    if (i == me || (barLock && locks[i])) {
                                        color[i] = progress;
                                        toHexWindow(hexWindow[i], color[i]);
                                        sb[i].setBackgroundColor(0xFF << 24 | (progress << (24 - i * 8)));
                                        sb[i].setProgress(progress);
                                    }
                                }
                            }
                        }

                        public void onStartTrackingTouch(SeekBar seekBar) {
                            doProgressChanged(seekBar, seekBar.getProgress(), true);
                        }

                        public void onStopTrackingTouch(SeekBar seekBar) {
                            doProgressChanged(seekBar, seekBar.getProgress(), true);
                        }
                    }
            );
            lk[i] = new CheckBox(context);
            lk[i].setLayoutParams(new LinearLayout.LayoutParams(WC, WC, 0));
            lk[i].setOnCheckedChangeListener(this);
            lk[i].setTag(i);
            lh.addView(tv);
            lh.addView(sb[i]);
            lh.addView(lk[i]);
            int FP = LinearLayout.LayoutParams.FILL_PARENT;
            lv.addView(lh, FP, WC);
        }
        {//Evaluating hex windows.
            LinearLayout lh = new LinearLayout(context);
            lh.setGravity(Gravity.CENTER);
            for (int i = 0; i < arraySizes; i++) {
                hexWindow[i] = new TextView(context);
                toHexWindow(hexWindow[i], color[i]);
                lh.addView(hexWindow[i]);
            }
            lv.addView(lh);
        }//Evaluating hex windows.
        ScrollView sv = new ScrollView(context);
        sv.addView(lv);
        dialog1.setView(sv);
        String Title = context.getString(R.string.addshortcut_make_text_icon);
        dialog1.setTitle(Title);
        dialog1.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.yes), (dialog, which) -> {
            int mColor = (color[0] << 24) | (color[1] << 16) | (color[2] << 8) | color[3];
            imgtext = value.getText().toString();
            result[1] = imgtext;
            imgview.setTag(mColor);
            if (!imgtext.equals("")) {
                imgview.setImageBitmap(
                        TextIcon.getTextIcon(
                                imgtext
                                , mColor
                                , 96
                                , 96
                        )
                );
            }
            dialog.dismiss();
            dialog.dismiss();
        });
        dialog1.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        dialog1.show();
        started = true;
    }

    public void toHexWindow(TextView tv, int k) {
        String HEX = "0123456789ABCDEF";
        String s = "";
        int n = 8;
        k &= (1L << 8) - 1L;
        for (n -= 4; n >= 0; n -= 4) s += HEX.charAt((k >> n) & 0xF);
        tv.setText(s);
    }

    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        int view = (Integer) buttonView.getTag();
        locks[view] = isChecked;
        barLock = false;
        for (int i = 0; i < locks.length; i++) if (locks[i]) barLock = true;
    }
}
