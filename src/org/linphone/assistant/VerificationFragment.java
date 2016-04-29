package org.linphone.assistant;
/*
LoginFragment.java
Copyright (C) 2015  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
import org.linphone.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;

import org.linphone.core.LinphoneAddress.TransportType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * @author Sylvain Berfini
 */
public class VerificationFragment extends Fragment {

    public String userName;
    public String password;

    public static Button mContinueButton;
    private TableLayout mMoreTable;
    private String mPassword;

    public static TextView mVerificationCode;
    private TextView mServerInfoSm;
    private TextView mServerInfoRosterVersion;
    private TextView mServerInfoCarbons;
    private TextView mServerInfoMam;
    private TextView mServerInfoCSI;
    private TextView mServerInfoBlocking;
    private TextView mServerInfoPep;
    private TextView mSessionEst;
    private TextView mOtrFingerprint;
    private ImageView mAvatar;
    private RelativeLayout mOtrFingerprintBox;
    private ImageButton mOtrFingerprintToClipboardButton;

    private String stringToSha1;
    private String sha1CodedString;


    private boolean mFetchingAvatar = false;

    public boolean makingPostRequest = false;

    private String countryCode;
    private String phoneNumber;
    private ProgressBar mProgressBar;
    private String xmlResponse;
    private RadioGroup transports;
    private Button apply;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.assistant_verification, container, false);
        countryCode = getArguments().getString("countryCode");
        phoneNumber = getArguments().getString("phoneNumber");

        userName = countryCode + phoneNumber;


        this.mPassword = getArguments().getString("phoneNumber");
        password = this.mPassword;



        this.mVerificationCode = (TextView) view.findViewById(R.id.verification_code);
        this.mContinueButton = (Button) view.findViewById(R.id.continue_button);

        this.mContinueButton.setOnClickListener(this.mContinueButtonClickListener);
        this.mProgressBar = (ProgressBar) view.findViewById(R.id.login_progress);

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        getActivity().getApplicationContext().registerReceiver(Receiver, filter);



        //   this.mRegisterNew.setOnCheckedChangeListener(OnCheckedShowConfirmPassword);

        return view;
    }

    public BroadcastReceiver Receiver = new BroadcastReceiver () {

        public final String action = "android.provider.Telephony.SMS_RECEIVED";
        public boolean smsReceived = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            int index;
            String verificationCode = "";
            Toast.makeText(getActivity(),"Acaba de recibir...",Toast.LENGTH_LONG);
            if (intent.getAction().equals(action)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < pdus.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                    for (SmsMessage message : messages) {

                        String strMessageFrom = message.getDisplayOriginatingAddress();
                        String strMessageBody = message.getDisplayMessageBody();

                        index = strMessageBody.indexOf("VERIFICACION");
                        verificationCode = strMessageBody.substring(index + 13, index + 17);
                        VerificationFragment.mVerificationCode.setText(verificationCode);
                        register(verificationCode);

                        //Toast para mostrar código de verificación extraido
                        //Toast.makeText(context, "From : " + strMessageFrom + "\nVerification coded : " + verificationCode, Toast.LENGTH_LONG).show();

                    }


                }
            }
        }
    };


    private final OnClickListener mContinueButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {

            //Toast.makeText(getActivity(), "Button clicked", Toast.LENGTH_LONG).show();
            if (mVerificationCode!=null)
            {
                register(mVerificationCode.getText().toString());
                //postRequest(mVerificationCode.getText().toString());
            }

            //Toast para mostrar password
            //Toast.makeText(getActivity(), "password on : "+ password, Toast.LENGTH_LONG).show();



        }
    };

    public void register(String verificationCode) {

        postRequest(verificationCode);
    }

    public void postRequest(String verificationCode) {
        String urlForPostRequest;

        //Toast.makeText(getActivity(), "Postrequest", Toast.LENGTH_LONG).show();
        stringToSha1 = countryCode + phoneNumber + verificationCode + "J&TqQpnMs4CJ56g";

        try {
            //Toast.makeText(getActivity(), "sha1code", Toast.LENGTH_LONG).show();
            sha1CodedString = sha1(stringToSha1);
        } catch (Exception e) {
            //Toast.makeText(getActivity(), "error in sha1code", Toast.LENGTH_LONG).show();
            sha1CodedString = stringToSha1;
        }

        /* Proceso asíncrono para registro */

        //Toast para cadena encriptada
        //Toast.makeText(getActivity(), sha1CodedString, Toast.LENGTH_LONG).show();

        urlForPostRequest = "http://sip.fututel.com/billing/api/user_register_mobile?u=admin&country_prefix=" + countryCode + "&local_number=" + phoneNumber + "&verif_code=" + verificationCode + "&hash=" + sha1CodedString;
        //new MyAsyncTask().execute("http://sip.fututel.com/billing/api/send_sms_verification_code?u=admin&country_prefix=57&local_number=3204837292&hash=ca2496f1b712be2aa275334b538a5c8e398d0cc1");
        makingPostRequest = true;
        //showProgress(true);
        new MyAsyncTask().execute(urlForPostRequest, verificationCode);


    }

    private void registerFinalStep () {

    }

    /**
     * Devuelve el código hash SHA-512 de la cádena de caracteres dada.
     *
     * @param s La cádena a codificar.
     * @return tel cçodigo SHA-512 de {@code s}.
     */
    private String sha1(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(s.getBytes());
        byte[] bytes = md.digest();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String tmp = Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1);
            buffer.append(tmp);
        }
        return buffer.toString();
    }

    private String downloadContent(String myurl, String verificationCode) throws IOException {

        /* Este método realiza la petición POST al servidor de sip.fututel.com */
        InputStream is = null;
        int length = 500;


        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("u", "admin")
                    .appendQueryParameter("country_prefix", countryCode)
                    .appendQueryParameter("local_number", phoneNumber)
                    .appendQueryParameter("verif_code", verificationCode)
                    .appendQueryParameter("hash", sha1CodedString);
            String query = builder.build().getEncodedQuery();


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            conn.connect();
            int response = conn.getResponseCode();

            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = convertInputStreamToString(is, length);

            xmlResponse = contentAsString;

            int index = contentAsString.indexOf("password");
            password = contentAsString.substring(index + 9, index + 19);



            return contentAsString;




            //return contentAsString;

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {

        /* Proceso asíncrono que llama a downloadContent (Función que realiza  la petición POST)*/

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub

            try {
                //showProgress(true);
                return downloadContent(params[0],params[1]);

            } catch (IOException e) {
                return "No se pudo establecer la conexión. La URL puede ser invalida";
                // Toast.makeText(getActivity(), xmlResponse, Toast.LENGTH_LONG).show();
            }

        }

        protected void onPostExecute(String result) {

            //Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
            //Toast.makeText(getActivity(), userName, Toast.LENGTH_LONG).show();

            //Toast para mostrar respuesta xml del servidor
            //Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
            //Toast.makeText(getActivity(), "password:"+password, Toast.LENGTH_LONG).show();


            if (result.contains("success")) {
                showProgress(false);
                Toast.makeText(getActivity(), "Registro satisfactorio", Toast.LENGTH_LONG).show();
                //showProgress(true);
                AssistantActivity.instance().genericLogIn(userName, password, "", "sip.fututel.com", TransportType.LinphoneTransportUdp);


                //finish();

            } else {
                Toast.makeText(getActivity(), "Se presentó un problema en el registro", Toast.LENGTH_LONG).show();
                //showProgress(false);
                makingPostRequest = false;
            }



        }

    }

    public String convertInputStreamToString(InputStream stream, int length) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[length];
        reader.read(buffer);
        return new String(buffer);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            final int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    mProgressBar.animate().setDuration(shortAnimTime).alpha(
                            show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

                }
            });


        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);

                }
            });


        }
    }
}
