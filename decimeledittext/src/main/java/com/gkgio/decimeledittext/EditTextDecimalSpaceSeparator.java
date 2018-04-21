package com.gkgio.decimeledittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;

import static com.gkgio.decimeledittext.Utils.isEmpty;

/**
 * Created by Gigauri on 15.04.18.
 * gkgio
 */

public class EditTextDecimalSpaceSeparator extends android.support.v7.widget.AppCompatEditText implements InputFilter, TextWatcher {
    private final static String TAG = EditTextDecimalSpaceSeparator.class.toString();

    private static final String SAVE_AMOUNT_EDIT_TEXT_STATE = "AMOUNT_EDIT_TEXT_STATE";
    private static final String SAVE_AMOUNT_LAST_LENGTH = "AMOUNT_LAST_LENGTH ";
    private static final String SAVE_AMOUNT_LAST_COUNT_SPACE = "AMOUNT_LAST_COUNT_SPACE";
    private static final String SAVE_SUPER_STATE = "SUPER_STATE";

    private int precision, scale;
    private boolean isFired = false;
    private int currentPosition = 0;
    private int lastLength = 0;
    private int lastCountSpace = 0;
    private int maxLength = 0;

    private String hintText = null;

    public EditTextDecimalSpaceSeparator(Context context) {
        super(context);
    }

    public EditTextDecimalSpaceSeparator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EditTextDecimalSpaceSeparator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.EditTextDecimalSpaceSeparator);
        precision = array.getInt(R.styleable.EditTextDecimalSpaceSeparator_integer_part, 20);
        scale = array.getInt(R.styleable.EditTextDecimalSpaceSeparator_decimal_part, 6);
        hintText = array.getString(R.styleable.EditTextDecimalSpaceSeparator_hint_text);
        maxLength = precision + scale + precision / 3 + 1;
        precision += scale;
        setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        this.setFilters(new InputFilter[]{this});
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    /**
     * getText without symbol that added for format display
     *
     * @return real value in string without format symbol
     */
    public String getValue() {
        if (!isEmpty(getText()) && getText().toString().contains(" ")) {
            return getText().toString().replaceAll(" ", "");
        }
        return getText().toString();
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        Log.d(TAG, "filter");
        if (!isFired && dest.toString().length() != 0) {
            for (int i = start; i < end; i++) {
                //not match pattern or start with dot(.)
                if (!String.valueOf(source.charAt(i)).matches("(\\d|\\.|,)*") || (dest.toString().length() == 0 && ".".equals(source.toString()))) {
                    return "";
                } else {
                    if (!isEmpty(dest)) {

                        //remove space if exist
                        String destVal = dest.toString();
                        if (destVal.contains(" ")) {
                            dstart -= (destVal.split(" ").length - 1);
                            destVal = destVal.replaceAll(" ", "");
                        }

                        if (".".equals(source.toString()) && destVal.contains("."))
                            return "";

                        if (".".equals(source.toString()))
                            if (destVal.contains(".") || dstart < 0 || destVal.substring(dstart).length() > scale)
                                return "";

                        //only precision without floating point
                        if (!".".equals(source.toString()) && destVal.matches("\\d{" + (precision - scale) + ",}"))
                            return "";

                        if (destVal.contains(".")) {
                            //scale limit (avoid move cursor when scale full by check only case dot before cursor)
                            if (destVal.matches("\\d*\\.\\d{" + scale + ",}"))
                                if (destVal.indexOf(".") < dstart)
                                    return "";

                            //precision limit (avoided move cursor when precision full by check only case dot after cursor)
                            if (destVal.matches("\\d{" + (precision - scale) + ",}\\.\\d*"))
                                if (destVal.indexOf(".") >= dstart)
                                    return "";
                        }
                    }
                }
            }
        } else if (dest.toString().length() == 0) {
            //check 0 input in empty string
            if ("0".equals(source.toString()) && dest.toString().length() == 0)
                return "";
        }
        return null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        Log.d(TAG, "beforeTextChanged");
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Log.d(TAG, "onTextChanged");

        if (!isEmpty(getText().toString()) && hintText != null) {
            setHint("");
        } else {
            lastLength = 0;
            setHint(hintText);
        }

        final int pointIndex = s.toString().indexOf('.');

        if (!isEmpty(s)) {
            //delete 0 if it in the start position
            if (s.toString().toCharArray()[0] == '0' && start == 0)
                setText(s.toString().substring(1));

            if (s.toString().contains(".")) {
                if (s.toString().length() > maxLength) {
                    if (pointIndex < maxLength - scale) {
                        deleteSuperfluousDecimalNumber(s, pointIndex + scale + 1);
                    } else {
                        deleteSuperfluousDecimalNumber(s, maxLength);
                    }
                } else if (s.toString().length() - (pointIndex + 1) > scale) {
                    deleteSuperfluousDecimalNumber(s, pointIndex + scale + 1);
                }
            } else if (s.toString().length() > maxLength - (scale + 1)) {
                deleteSuperfluousDecimalNumber(s, maxLength - (scale + 1));
            }
        }

        if (!isFired) {
            isFired = true;

            //remove space if exist
            String text = getText().toString();
            if (text.contains(" ")) text = text.replaceAll(" ", "");
            currentPosition = start;
            if (text.matches("\\d+\\.\\d+")) {
                setText(insertNumberSpaceSeparator(text.split("\\.")[0]) + "." + text.split("\\.")[1]);
            } else if (text.contains(".")) {
                setText(insertNumberSpaceSeparator(text.replace(".", "")) + ".");
            } else {
                setText(insertNumberSpaceSeparator(text));
            }
        } else {
            int countSpace = 0;
            char[] amountCharArray = getText().toString().toCharArray();
            for (char anAmountCharArray : amountCharArray) {
                if (anAmountCharArray == ' ') countSpace++;
            }
            isFired = false;
            if (getText().length() > lastLength) {
                lastLength = getText().length();
                if (countSpace > lastCountSpace) {
                    setSelection(currentPosition + 2);
                } else {
                    setSelection(currentPosition + 1);
                }
            } else if (getText().length() != 0 && getText().length() < lastLength) {
                lastLength = getText().length();
                if (currentPosition != 0 && lastCountSpace > countSpace) {
                    setSelection(currentPosition - 1);
                } else setSelection(currentPosition);
            } else if (getText().length() != 0 && getText().length() == lastLength) {
                setSelection(currentPosition);
            }
            lastCountSpace = countSpace;
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.d(TAG, "afterTextChanged");
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SAVE_SUPER_STATE, super.onSaveInstanceState());
        bundle.putString(SAVE_AMOUNT_EDIT_TEXT_STATE, getText().toString());
        bundle.putInt(SAVE_AMOUNT_LAST_LENGTH, lastLength);
        bundle.putInt(SAVE_AMOUNT_LAST_COUNT_SPACE, lastCountSpace);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable(SAVE_SUPER_STATE);
            lastLength = bundle.getInt(SAVE_AMOUNT_LAST_LENGTH);
            lastCountSpace = bundle.getInt(SAVE_AMOUNT_LAST_COUNT_SPACE);
            setText(bundle.getString(SAVE_AMOUNT_EDIT_TEXT_STATE));
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * @param normalNumber unformat number
     * @return formatted number
     */
    private String insertNumberSpaceSeparator(String normalNumber) {
        if (isEmpty(normalNumber)) return "";

        if (normalNumber.length() > 3) {
            StringBuilder numberReverse = new StringBuilder();
            normalNumber = normalNumber.replaceAll(" ", "");
            for (int i = normalNumber.length() - 1, count = 1; i >= 0; --i, ++count) {
                numberReverse.append(normalNumber.charAt(i));
                if (count == 3 && i != 0) {
                    numberReverse.append(" ");
                    count = 0;
                }
            }
            return numberReverse.reverse().toString();
        } else {
            return normalNumber;
        }
    }

    private void deleteSuperfluousDecimalNumber(CharSequence s, int maxLength) {
        setText(s.toString().substring(0, maxLength));
    }
}
