package co.hodlwallet.presenter.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import co.hodlwallet.R;
import co.hodlwallet.presenter.activities.settings.WebViewActivity;
import co.hodlwallet.presenter.customviews.BRButton;
import co.hodlwallet.presenter.customviews.BRDialogView;
import co.hodlwallet.presenter.customviews.BRKeyboard;
import co.hodlwallet.presenter.customviews.BRLinearLayoutWithCaret;
import co.hodlwallet.presenter.customviews.BRText;
import co.hodlwallet.presenter.entities.PaymentItem;
import co.hodlwallet.presenter.entities.RequestObject;
import co.hodlwallet.tools.animation.BRAnimator;
import co.hodlwallet.tools.animation.BRDialog;
import co.hodlwallet.tools.animation.SlideDetector;
import co.hodlwallet.tools.animation.SpringAnimator;
import co.hodlwallet.tools.manager.BRClipboardManager;
import co.hodlwallet.tools.manager.BRSharedPrefs;
import co.hodlwallet.tools.security.BitcoinUrlHandler;
import co.hodlwallet.tools.security.BRSender;
import co.hodlwallet.tools.threads.BRExecutor;
import co.hodlwallet.tools.util.BRConstants;
import co.hodlwallet.tools.util.BRExchange;
import co.hodlwallet.tools.util.BRCurrency;
import co.hodlwallet.tools.util.TrustedNode;
import co.hodlwallet.tools.util.Utils;
import co.hodlwallet.wallet.BRPeerManager;
import co.hodlwallet.wallet.BRWalletManager;

import java.math.BigDecimal;

import static co.hodlwallet.tools.security.BitcoinUrlHandler.getRequestFromString;
import static co.platform.HTTPServer.URL_SUPPORT;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentSend extends Fragment {
    private static final String TAG = FragmentSend.class.getName();
    public ScrollView backgroundLayout;
    public LinearLayout signalLayout;
    private BRKeyboard keyboard;
    private EditText addressEdit;
    private ImageButton scan;
    private ImageButton paste;
    private Button send;
    private EditText commentEdit;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;
    private TextView balanceText;
    private long curBalance;
    private String selectedIso;
    private ImageButton isoButton;
    private TextView isoButtonText;
    private int keyboardIndex;
    private LinearLayout keyboardLayout;
    private ImageButton close;
    private ConstraintLayout amountLayout;;
    private SeekBar feeSlider;
    private TextView feeText;
    private TextView currentTime;
    private BRLinearLayoutWithCaret feeLayout;
    private Button customFee;
    private AlertDialog customDialog;
    private boolean feeButtonsShown = true;
    public static boolean isEconomyFee;
    private boolean amountLabelOn = true;
    private boolean didSet = false;
    private boolean balanceShown = false;

    private static String savedMemo;
    private static String savedIso;
    private static String savedAmount;

    private boolean ignoreCleanup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_send, container, false);
        backgroundLayout = (ScrollView) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_dark_button);
        keyboard.setBRKeyboardColor(R.color.gray_background);
        isoText = (TextView) rootView.findViewById(R.id.iso_text);
        addressEdit = (EditText) rootView.findViewById(R.id.address_edit);
        scan = (ImageButton) rootView.findViewById(R.id.scan);
        paste = (ImageButton) rootView.findViewById(R.id.paste_button);
        send = (Button) rootView.findViewById(R.id.send_button);
        commentEdit = (EditText) rootView.findViewById(R.id.comment_edit);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        balanceText = (TextView) rootView.findViewById(R.id.balance_text);
        isoButton = (ImageButton) rootView.findViewById(R.id.iso_button);
        isoButtonText = (TextView) rootView.findViewById(R.id.iso_button_text);
        keyboardLayout = (LinearLayout) rootView.findViewById(R.id.keyboard_layout);
        amountLayout = (ConstraintLayout) rootView.findViewById(R.id.amount_layout);
        feeLayout = (BRLinearLayoutWithCaret) rootView.findViewById(R.id.fee_buttons_layout);

        feeSlider = (SeekBar) rootView.findViewById(R.id.seek_bar);
        feeText = (TextView) rootView.findViewById(R.id.fee_text);
        currentTime = (TextView) rootView.findViewById(R.id.current_time);
        close = (ImageButton) rootView.findViewById(R.id.close_button);
        selectedIso = BRSharedPrefs.getPreferredBTC(getContext()) ? "BTC" : BRSharedPrefs.getIso(getContext());
        customFee = (Button) rootView.findViewById(R.id.custom_fee);

        amountBuilder = new StringBuilder(0);
        showBalance(balanceShown);
        setListeners();
        isoText.setText(getString(R.string.Send_amountLabel));
        isoText.setTextSize(18);
        isoText.setTextColor(getContext().getColor(R.color.light_gray));
        isoText.requestLayout();

        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        keyboardIndex = signalLayout.indexOfChild(keyboardLayout);

        ImageButton faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }
                BRAnimator.showSupportFragment(app, BRConstants.send);
            }
        });

        showKeyboard(false);

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        updateText();

        return rootView;
    }

    private void setListeners() {
        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(true);
                if (amountLabelOn) { //only first time
                    amountLabelOn = false;
                    amountEdit.setHint("0");
                    amountEdit.setTextSize(24);
                    amountEdit.setHintTextColor(getContext().getColor(R.color.logo_gradient_start));
                    // balanceText.setVisibility(View.VISIBLE);
                    isoText.setTextColor(getContext().getColor(R.color.logo_gradient_start));
                    isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
                    isoText.setTextSize(28);
                    final float scaleX = amountEdit.getScaleX();
                    amountEdit.setScaleX(0);

                    AutoTransition tr = new AutoTransition();
                    tr.setInterpolator(new OvershootInterpolator());
                    tr.addListener(new android.support.transition.Transition.TransitionListener() {
                        @Override
                        public void onTransitionStart(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionEnd(@NonNull android.support.transition.Transition transition) {
                            amountEdit.requestLayout();
                            amountEdit.animate().setDuration(100).scaleX(scaleX);
                        }

                        @Override
                        public void onTransitionCancel(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionPause(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionResume(@NonNull android.support.transition.Transition transition) {

                        }
                    });

                    ConstraintSet set = new ConstraintSet();
                    set.clone(amountLayout);
                    TransitionManager.beginDelayedTransition(amountLayout, tr);

                    set.applyTo(amountLayout);
                    balanceShown = !balanceShown;
                    showBalance(balanceShown);
                }

            }
        });

        //needed to fix the overlap bug
        commentEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    amountLayout.requestLayout();
                    return true;
                }
                return false;
            }
        });

        addressEdit.addTextChangedListener(new TextWatcher() {
               @Override
               public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

               @Override
               public void onTextChanged(CharSequence s, int start, int before, int count) { }

               @Override
               public void afterTextChanged(Editable s) {
                    // Here's an easier way to know if the content was pasted.
                    // Check if what changed is the content of the clipboard, we can assume that's a paste.
                    String clipboardContent = BRClipboardManager.getClipboard(getActivity());
                    if (clipboardContent.equals(s.toString())) {
                        if (s.toString().startsWith("bitcoin:")) {
                            Log.d(TAG, "afterTextChanged: Processing clipboard content: " + s.toString());

                            RequestObject obj = getRequestFromString(s.toString());

                            String address = "";
                            String amount = "";
                            String label = "";
                            String message = "";
                            String memo = "";

                            if (obj.r != null) {
                                // Check for compatibility mode if address and amount are present
                                if (obj.address != null && obj.amount != null) {
                                    address = obj.address;
                                    amount = obj.amount;
                                } else {
                                    BitcoinUrlHandler.processRequest(getActivity(), s.toString());

                                    return;
                                }
                            } else {
                                address = obj.address;
                                amount = obj.amount;
                                label = obj.label;
                                message = obj.message;
                            }

                            // if address is null at this point, we fail.

                            // Build the label and the message into a nice memo: "label: Message"
                            if (label != null && !label.isEmpty())
                                memo += label;

                            if (message != null && !message.isEmpty())
                                if (label != null && !label.isEmpty()) // if it has label then add a ": "
                                    memo += ": ";

                                memo += message;

                            addressEdit.setText(address);

                            if (amount != null && !amount.isEmpty())
                                updateAmountWithSatoshis(amount);

                            if (memo != null && !memo.isEmpty())
                                commentEdit.setText(memo);

                            return;
                        } else if (BRWalletManager.validateAddress(s.toString())) {
                            // First we check if the address is already used. (e.g. send to yourself)
                            final String address = s.toString();
                            final BRWalletManager walletManager = BRWalletManager.getInstance();
                            final Context app = getContext();

                            if (app == null) {
                                Log.e(TAG, "paste onClick: app is null");
                                return;
                            }

                            if (walletManager.addressContainedInWallet(address)) {
                                BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_containsAddress), getResources().getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismiss();
                                        addressEdit.setText("");
                                    }
                                }, null, null, 0);
                            } else if (walletManager.addressIsUsed(address)) {
                                BRDialog.showCustomDialog(getActivity(), getString(R.string.Send_UsedAddress_firstLine), getString(R.string.Send_UsedAddress_secondLIne), "Ignore", "Cancel", new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismiss();
                                    }
                                }, new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismiss();
                                        addressEdit.setText("");
                                    }
                                }, null, 0);
                            } else {
                                // Address is valid.. we get out.
                                return;
                            }
                        } else { // error, it's not "bitcoin:" or a valid address
                            showClipboardError();

                            return;
                        }
                    }
               }
           });

            paste.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!BRAnimator.isClickAllowed()) return;

                    String bitcoinUrl = BRClipboardManager.getClipboard(getActivity());
                    addressEdit.setText(bitcoinUrl);
                }
            });

        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getIso(getContext()))) {
                    if (amountBuilder.length() > 0) {
                        BigDecimal tmpAmount = BRExchange.getSatoshisFromAmount(getContext(), selectedIso, new BigDecimal(amountBuilder.toString()));

                        if (BRSharedPrefs.getCurrencyUnit(getContext()) == BRConstants.CURRENT_UNIT_BITCOINS)
                            tmpAmount = BRExchange.getBitcoinForSatoshis(getContext(), tmpAmount);

                        amountBuilder = new StringBuilder(tmpAmount.toString());
                        amountEdit.setText(amountBuilder.toString());
                    }

                    selectedIso = "BTC";
                } else {
                    if (amountBuilder.length() > 0) {
                        BigDecimal currentAmountInSatoshi = null;

                        if (BRSharedPrefs.getCurrencyUnit(getContext()) == BRConstants.CURRENT_UNIT_BITCOINS) {
                            currentAmountInSatoshi = new BigDecimal(amountBuilder.toString()).multiply(new BigDecimal(100000000));
                        } else {
                            currentAmountInSatoshi = new BigDecimal(amountBuilder.toString());
                        }

                        BigDecimal tmpAmount = BRExchange.getAmountFromSatoshis(getContext(), BRSharedPrefs.getIso(getContext()), currentAmountInSatoshi);

                        amountBuilder = new StringBuilder(tmpAmount.toString());
                        amountEdit.setText(amountBuilder.toString());
                    }

                    selectedIso = BRSharedPrefs.getIso(getContext());
                }
                updateText();

            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                saveMetaData();
                BRAnimator.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //not allowed now
                if (!BRAnimator.isClickAllowed()) {
                    return;
                }

                boolean allFilled = true;
                String address = addressEdit.getText().toString();
                String amountStr = amountBuilder.toString();
                String iso = selectedIso;
                String comment = commentEdit.getText().toString();

                //get amount in satoshis from any isos
                BigDecimal bigAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) ? "0" : amountStr);
                BigDecimal satoshiAmount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount);

                if (address.isEmpty() || !BRWalletManager.validateAddress(address)) {
                    allFilled = false;
                    Activity app = getActivity();
                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_noAddress), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                }
                if (satoshiAmount.doubleValue() < 1) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), amountEdit);
                }
                if (satoshiAmount.longValue() > BRWalletManager.getInstance().getBalance(getActivity())) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), balanceText);
                }

                if (allFilled)
                    BRSender.getInstance().sendTransaction(getContext(), new PaymentItem(new String[]{address}, null, satoshiAmount.longValue(), null, false, comment));
            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                if (app != null)
                    app.getFragmentManager().popBackStack();
            }
        });


        addressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    Utils.hideKeyboard(getActivity());
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showKeyboard(true);
                        }
                    }, 500);

                }
                return false;
            }
        });

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });

        feeSlider.setMax(500);
        feeSlider.setProgress(250);
        final int divisor = feeSlider.getMax() / 4;
        long normal = BRSharedPrefs.getFeePerKb(getContext()) / 1000L;
        feeText.setText(String.format(getString(R.string.FeeSelector_satByte), normal));
        currentTime.setText(BRSharedPrefs.getFeeTimeText(getContext()));

        feeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentFee = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFee = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long fee = 0;
                switch (currentFee / divisor) {
                    case 0:
                        fee = BRSharedPrefs.getLowFeePerKb(getContext());
                        currentTime.setText(BRSharedPrefs.getEconomyFeeTimeText(getContext()));
                        feeSlider.setProgress(0);
                        break;
                    case 1:
                        fee = BRSharedPrefs.getFeePerKb(getContext());
                        currentTime.setText(BRSharedPrefs.getFeeTimeText(getContext()));
                        feeSlider.setProgress(divisor * 2);
                        break;
                    case 2:
                        fee = BRSharedPrefs.getFeePerKb(getContext());
                        currentTime.setText(BRSharedPrefs.getFeeTimeText(getContext()));
                        feeSlider.setProgress(divisor * 2);
                        break;
                    case 3:
                        fee = BRSharedPrefs.getHighFeePerKb(getContext());
                        currentTime.setText(BRSharedPrefs.getHighFeeTimeText(getContext()));
                        feeSlider.setProgress(feeSlider.getMax());
                        break;
                    case 4:
                        fee = BRSharedPrefs.getHighFeePerKb(getContext());
                        currentTime.setText(BRSharedPrefs.getHighFeeTimeText(getContext()));
                        feeSlider.setProgress(feeSlider.getMax());
                        break;
                }
                BRWalletManager.getInstance().setFeePerKb(fee, false);
                feeText.setText(String.format(getString(R.string.FeeSelector_satByte), (fee / 1000L)));
                updateText();
            }
        });

        customFee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                createDialog();
            }
        });

    }

    private void updateAmountWithSatoshis(String amount) {
        // 0. clear what we currently have in amount
        amountBuilder.replace(0, amountBuilder.length(), "");

        if (!selectedIso.equals("BTC")) {
            isoButton.performClick();
        }

        if (BRSharedPrefs.getCurrencyUnit(getContext()) == BRConstants.CURRENT_UNIT_BITCOINS) {
            amount = new BigDecimal(amount).divide(new BigDecimal("100000000")).toString();
        } else {
            amount = new BigDecimal(amount).toBigInteger().toString();
        }

        amountBuilder.replace(0, amount.length(), amount);

        updateText();
    }

    private void showKeyboard(boolean b) {
        int curIndex = keyboardIndex;

        if (!b) {
            signalLayout.removeView(keyboardLayout);

        } else {
            Utils.hideKeyboard(getActivity());
            if (signalLayout.indexOfChild(keyboardLayout) == -1)
                signalLayout.addView(keyboardLayout, curIndex);
            else
                signalLayout.removeView(keyboardLayout);

        }
    }

    private void showClipboardError() {
        BRDialog.showCustomDialog(getActivity(), getString(R.string.Send_emptyPasteboard), getResources().getString(R.string.Send_invalidAddressTitle), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                BRAnimator.animateBackgroundDim(backgroundLayout, false);
                BRAnimator.animateSignalSlide(signalLayout, false, new BRAnimator.OnSlideAnimationEnd() {
                    @Override
                    public void onAnimationEnd() {
                        Bundle bundle = getArguments();
                        if (bundle != null && bundle.getString("url") != null)
                            setUrl(bundle.getString("url"));
                    }
                });
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
        BRAnimator.animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null) {
                    try {
                        getActivity().getFragmentManager().popBackStack();
                    } catch (Exception ignored) {

                    }
                }
            }
        });
        isEconomyFee = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMetaData();

    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
        if (!ignoreCleanup) {
            savedIso = null;
            savedAmount = null;
            savedMemo = null;
        }
    }

    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= BRExchange.getMaxAmount(getActivity(), iso).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > BRCurrency.getMaxDecimalPlaces(getContext(), iso))))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || BRCurrency.getMaxDecimalPlaces(getContext(), selectedIso) == 0)
            return;
        amountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }

    }

    private void updateText() {
        if (getActivity() == null) return;
        String tmpAmount = amountBuilder.toString();
        setAmount();
        String balanceString;
        String iso = selectedIso;
        curBalance = BRWalletManager.getInstance().getBalance(getActivity());
        if (!amountLabelOn)
            isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
        isoButtonText.setText(String.format("%s(%s)", BRCurrency.getCurrencyName(getActivity(), selectedIso), BRCurrency.getSymbolByIso(getActivity(), selectedIso)));
        //Balance depending on ISO
        long satoshis = (Utils.isNullOrEmpty(tmpAmount) || tmpAmount.equalsIgnoreCase(".")) ? 0 :
                (selectedIso.equalsIgnoreCase("btc") ? BRExchange.getSatoshisForBitcoin(getActivity(), new BigDecimal(tmpAmount)).longValue() : BRExchange.getSatoshisFromAmount(getActivity(), selectedIso, new BigDecimal(tmpAmount)).longValue());
        BigDecimal balanceForISO = BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(curBalance));

        //formattedBalance
        String formattedBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, balanceForISO);
        //Balance depending on ISO
        long fee = 0;
        if (satoshis == 0) {
            fee = 0;
        } else {
            String address = addressEdit.getText().toString();
            if (!address.isEmpty() && BRWalletManager.validateAddress(address))
                fee = BRWalletManager.getInstance().feeForTransaction(addressEdit.getText().toString(), satoshis);
            if (fee == 0) {
                fee = BRWalletManager.getInstance().feeForTransactionAmount(satoshis);
            }
        }

        BigDecimal feeForISO = BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(fee));
        //formattedBalance
        String aproxFee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, feeForISO);
        if (new BigDecimal((tmpAmount.isEmpty() || tmpAmount.equalsIgnoreCase(".")) ? "0" : tmpAmount).doubleValue() > balanceForISO.doubleValue()) {
            balanceText.setTextColor(getContext().getColor(R.color.warning_color));
            amountEdit.setTextColor(getContext().getColor(R.color.warning_color));
            if (!amountLabelOn)
                isoText.setTextColor(getContext().getColor(R.color.warning_color));
        } else {
            balanceText.setTextColor(getContext().getColor(R.color.light_gray));
            amountEdit.setTextColor(getContext().getColor(R.color.logo_gradient_start));
            if (!amountLabelOn)
                isoText.setTextColor(getContext().getColor(R.color.logo_gradient_start));
        }
        if (!tmpAmount.isEmpty()) {
            feeText.setText(aproxFee);
            didSet = true;
        } else if (tmpAmount.isEmpty() && didSet) {
            feeText.setText(aproxFee);
            didSet = false;
        }
        balanceString = String.format(getString(R.string.Send_balance), formattedBalance);
        balanceText.setText(String.format("%s", balanceString));
        amountLayout.requestLayout();
    }

    public void setUrl(String url) {
        RequestObject obj = BitcoinUrlHandler.getRequestFromString(url);
        if (obj == null) return;
        if (obj.address != null && addressEdit != null) {
            addressEdit.setText(obj.address.trim());
        }
        if (obj.message != null && commentEdit != null) {
            commentEdit.setText(obj.message);
        }
        if (obj.amount != null) {
            String iso = selectedIso;
            BigDecimal satoshiAmount = new BigDecimal(obj.amount).multiply(new BigDecimal(100000000));
            amountBuilder = new StringBuilder(BRExchange.getAmountFromSatoshis(getActivity(), iso, satoshiAmount).toPlainString());
            updateText();

        }
    }

    private void showBalance(boolean b) {
        if (!b) {
            feeLayout.removeView(balanceText);
        } else {
            feeLayout.addView(balanceText, 1);
        }
    }

    private void setAmount() {
        String tmpAmount = amountBuilder.toString();
        int divider = tmpAmount.length();
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".");
        }
        StringBuilder newAmount = new StringBuilder();
        for (int i = 0; i < tmpAmount.length(); i++) {
            newAmount.append(tmpAmount.charAt(i));
            if (divider > 3 && divider - 1 != i && divider > i && ((divider - i - 1) % 3 == 0)) {
                newAmount.append(",");
            }
        }
        amountEdit.setText(newAmount.toString());
    }

    // from the link above
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Log.e(TAG, "onConfigurationChanged: hidden");
            showKeyboard(true);
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Log.e(TAG, "onConfigurationChanged: shown");
            showKeyboard(false);
        }
    }

    private void saveMetaData() {
        if (!commentEdit.getText().toString().isEmpty())
            savedMemo = commentEdit.getText().toString();
        if (!amountBuilder.toString().isEmpty())
            savedAmount = amountBuilder.toString();
        savedIso = selectedIso;
        ignoreCleanup = true;
    }

    private void loadMetaData() {
        ignoreCleanup = false;
        if (!Utils.isNullOrEmpty(savedMemo))
            commentEdit.setText(savedMemo);
        if (!Utils.isNullOrEmpty(savedIso))
            selectedIso = savedIso;
        if (!Utils.isNullOrEmpty(savedAmount)) {
            amountBuilder = new StringBuilder(savedAmount);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    amountEdit.performClick();
                    updateText();
                }
            }, 500);

        }
    }

    private void createDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        final TextView customTitle = new TextView(getActivity());

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(getActivity(), 32);
        int pad16 = Utils.getPixelsFromDps(getActivity(), 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.FeeSelector_customFee));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.FeeSelector_customBody));

        final EditText input = new EditText(getActivity());
        input.setHint(getString(R.string.FeeSelector_customHint));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(getActivity(), 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        customDialog = alertDialog.show();

        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
        customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = input.getText().toString();
                if (str.isEmpty()) {
                    str = "0";
                }
                long fee = Long.parseLong(str) * BRConstants.BYTE_SHIFT;
                if (fee < BRConstants.FEE_LIMIT) {
                    BRWalletManager.getInstance().setFeePerKb(fee, false);
                    if (amountBuilder.toString().isEmpty()) {
                        feeText.setText(String.format(getString(R.string.FeeSelector_satByte), (fee / 1000L)));
                    } else {
                        updateText();
                    }
                    customDialog.dismiss();
                } else {
                    customTitle.setText(getString(R.string.Alert_error));
                    customDialog.setMessage(getString(R.string.FeeSelector_customError));
                    input.setCursorVisible(false);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            customTitle.setText(getString(R.string.FeeSelector_customFee));
                            customDialog.setMessage(getString(R.string.FeeSelector_customBody));
                            input.setText("");
                            input.setCursorVisible(true);
                        }
                    }, 2000);
                }
            }
        });
    }

}
