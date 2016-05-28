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
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import org.linphone.core.LinphoneAddress.TransportType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * @author Sylvain Berfini
 */
public class VerificationFragment extends Fragment {

    public static Button mContinueButton;

    public static TextView mVerificationCode;

    private String stringToSha1;
    private String sha1CodedString;

    public boolean makingPostRequest = false;

    private String countryCode;
    private String phoneNumber;
    private ProgressBar mProgressBar;
    private boolean success = true;
    private String textresponse = null;
    private String password = null;
    private String username = null;
    private ProgressDialog dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.assistant_verification, container, false);
        countryCode = getArguments().getString("countryCode");
        phoneNumber = getArguments().getString("phoneNumber");

        this.mVerificationCode = (TextView) view.findViewById(R.id.verification_code);
        this.mContinueButton = (Button) view.findViewById(R.id.continue_button);

        this.mContinueButton.setOnClickListener(this.mContinueButtonClickListener);
        this.mProgressBar = (ProgressBar) view.findViewById(R.id.login_progress);

        //IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        //getActivity().getApplicationContext().registerReceiver(Receiver, filter);



        //   this.mRegisterNew.setOnCheckedChangeListener(OnCheckedShowConfirmPassword);

        return view;
    }
 /*
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
    */


    private final OnClickListener mContinueButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {

            boolean validation = true;
            String urlForPostRequest;

            if (!makingPostRequest)
            {
                String verificationCode = mVerificationCode.getText().toString();
                if (verificationCode.equals("")) {
                    mVerificationCode.setError(getString(R.string.invalid_verification_code));
                    mVerificationCode.requestFocus();
                    validation = false;
                }

                stringToSha1 = countryCode + phoneNumber + verificationCode + "J&TqQpnMs4CJ56g";

                try {
                    //Toast.makeText(getActivity(), "sha1code", Toast.LENGTH_LONG).show();
                    sha1CodedString = sha1(stringToSha1);
                } catch (Exception e) {
                    //Toast.makeText(getActivity(), "error in sha1code", Toast.LENGTH_LONG).show();
                    sha1CodedString = stringToSha1;
                }


                if (validation)
                {
                 /* Proceso asíncrono para registro nuevo o existente */

                    urlForPostRequest = "http://sip.fututel.com/billing/api/user_register_mobile?u=admin&country_prefix=" + countryCode + "&local_number=" + phoneNumber + "&verif_code=" + verificationCode + "&hash=" + sha1CodedString;
                     makingPostRequest = true;

                    //Ejecuta el proceso asíncrono para la petición post
                    new MyAsyncTask().execute(urlForPostRequest);

                }

            }
        }
    };

    private void downloadContent(String myurl) throws IOException {

        /* Este método realiza la petición POST al servidor de FUTUTEL y procesa la respuesta */
        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            InputStream in = new BufferedInputStream(conn.getInputStream());

            String mytext = null;

            try {
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();

                myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                myparser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);
                myparser.setInput(in,null);

                int texthere = 0;
                int event = myparser.getEventType();

                while (event != XmlPullParser.END_DOCUMENT) {

                    switch (myparser.getEventType()) {
                        case XmlPullParser.START_TAG:
                            if (myparser.getName().equals("success")) {
                                texthere = 1;
                                success = true;
                                break;
                            } else if (myparser.getName().equals("error")) {
                                texthere = 1;
                                success = false;
                                break;
                            } else if (myparser.getName().equals("username")) {
                                texthere = 2;
                                break;
                            } else if (myparser.getName().equals("password")) {
                                texthere = 3;
                                break;
                            }
                            break;
                        case XmlPullParser.TEXT:
                            if (texthere == 1) {
                                mytext = myparser.getText();
                            }
                            else if (texthere == 2){
                                username = myparser.getText();
                            }
                            else if(texthere == 3){
                                password = myparser.getText();
                            }
                            texthere = 0;
                            break;

                        case XmlPullParser.END_TAG:
                            break;
                    }
                    event = myparser.next();

                }

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            textresponse = mytext;

        } finally {
            conn.disconnect();
        }
    }

    private class MyAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute(){
            dialog = new ProgressDialog(getActivity());
            dialog.setTitle(getString(R.string.registration_in_fututel));
            dialog.setMessage(getString(R.string.contacting_fututel_server_to_make_registration));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        /* Proceso asíncrono que llama a downloadContent (Función que realiza  la petición POST y convierte respueta XML a formatos aceptables)*/
        @Override
        protected Void doInBackground(String... params) {
            // TODO Auto-generated method stub

            try {
                downloadContent(params[0]);
                return null;

            } catch (IOException e) {
                textresponse = getString(R.string.internet_connection_error);
                return null;
            }

        }


        /* Proceso asíncrono que llama a downloadContent (Función que realiza  la petición POST)*/
        @Override
        protected void onPostExecute(Void v) {

            if (success) {
                dialog.dismiss();
                Toast.makeText(getActivity(), textresponse, Toast.LENGTH_LONG).show();
                AssistantActivity.instance().genericLogIn(username, password, "", "sip.fututel.com", TransportType.LinphoneTransportUdp);
            } else {
                dialog.dismiss();
                Toast.makeText(getActivity(), textresponse, Toast.LENGTH_LONG).show();
                makingPostRequest = false;
            }
        }

    }

    /**
     * Shows the progress UI and hides the login form.

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
*/
    /**
     * Devuelve el código hash SHA-512 de la cádena de caracteres dada.
     *
     * @string La cádena a codificar.
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
}
