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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.linphone.R;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.io.BufferedInputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

/**
 * @author Sylvain Berfini
 */
public class RegisterFragment extends Fragment {

	/*Variables agregadas */

	private Spinner mCountry;
	private EditText mPhone;
	private String countryCode;
	private EditText mCountryCodeText;
	private ProgressBar mProgressBar;

    private String stringToSha1;
	private String sha1CodedString;
	public boolean makingPostRequest = false;
    private boolean success = false;
    private String textresponse = null;
	private ProgressDialog dialog;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.assistant_register, container, false);

		   /* Definiciones de objetos de acuerdo a elementos de UI */


		this.mCountry = (Spinner) view.findViewById(R.id.country);
		this.mPhone = (EditText) view.findViewById(R.id.phone);
		this.mCountryCodeText = (EditText) view.findViewById(R.id.prefix);
		this.mProgressBar = (ProgressBar) view.findViewById(R.id.login_progress);

		//this.mMoreTable = (TableLayout) view.findViewById(R.id.server_info_more);


        Button mRegisterButton = (Button) view.findViewById(R.id.register_button);
        Button mHaveCodeButton = (Button) view.findViewById(R.id.already_have_code_button);
        Button mLoginButton = (Button) view.findViewById(R.id.login_button);

		mRegisterButton.setOnClickListener(this.mRegisterButtonClickListener);
        mHaveCodeButton.setOnClickListener(this.mHaveCodeButtonClickListener);
		mLoginButton.setOnClickListener(this.mLoginButtonClickListener);


		Spinner spinner = (Spinner) view.findViewById(R.id.country);
// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
				R.array.countries, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
		spinner.setAdapter(adapter);

		int spinnerPosition = adapter.getPosition("Colombia");

//set the default according to value
		spinner.setSelection(spinnerPosition);
		countryCode = getResources().getStringArray(R.array.prefixes)[spinnerPosition];

		mCountryCodeText.setText(countryCode);

		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				Object item = parent.getItemAtPosition(pos);
				countryCode = getResources().getStringArray(R.array.prefixes)[pos];
				mCountryCodeText.setText(countryCode);
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});


		return view;
	}

    private final OnClickListener mRegisterButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {

            boolean validation = true;
            String urlForPostRequest;

            if (!makingPostRequest)
            {


                if (mPhone.getText().toString().equals("")) {
                    mPhone.setError(getString(R.string.invalid_phone_number));
                    mPhone.requestFocus();
                    validation = false;
                }

                stringToSha1 = countryCode+mPhone.getText().toString()+"J&TqQpnMs4CJ56g";

                try {
                    sha1CodedString = sha1(stringToSha1);
                }
                catch (Exception e)
                {
                    sha1CodedString = stringToSha1;
                }


                if (validation)
                {
                 /* Proceso asíncrono para envío de SMS */


                    urlForPostRequest = "http://sip.fututel.com/billing/api/send_sms_verification_code?u=admin&country_prefix="+countryCode+"&local_number="+mPhone.getText().toString()+"&hash="+sha1CodedString;
                    makingPostRequest = true;

                    //Ejecuta el proceso asíncrono para la petición post
                    new MyAsyncTask().execute(urlForPostRequest);
                }

            }
        }


    };

	private final OnClickListener mHaveCodeButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(final View v) {

			boolean validation = true;

            if (mPhone.getText().toString().equals("")) {
                mPhone.setError(getString(R.string.invalid_phone_number));
                mPhone.requestFocus();
                validation = false;
            }

            if (validation)
            {
				Toast.makeText(getActivity(), getString(R.string.write_verification_code), Toast.LENGTH_LONG).show();
                AssistantActivity.instance().verificateCode(mPhone.getText().toString(), countryCode);
            }

		}

	};

    private final OnClickListener mLoginButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {

            AssistantActivity.instance().displayLoginGeneric();

        }

    };

    /*
	@Override
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.assistant_apply) {
			if (login.getText() == null || login.length() == 0 || password.getText() == null || password.length() == 0 || domain.getText() == null || domain.length() == 0) {
				Toast.makeText(getActivity(), getString(R.string.first_launch_no_login_password), Toast.LENGTH_LONG).show();
				return;
			}

			TransportType transport;
			if(transports.getCheckedRadioButtonId() == R.id.transport_udp){
				transport = TransportType.LinphoneTransportUdp;
			} else {
				if(transports.getCheckedRadioButtonId() == R.id.transport_tcp){
					transport = TransportType.LinphoneTransportTcp;
				} else {
					transport = TransportType.LinphoneTransportTls;
				}
			}

			AssistantActivity.instance().genericLogIn(login.getText().toString(), password.getText().toString(), displayName.getText().toString(), domain.getText().toString(), transport);
		}
	}
    */

	private void downloadContent(String myurl) throws IOException {

        /* Este método realiza la petición POST al servidor de FUTUTEL */
		URL url = new URL(myurl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream in = null;
		try {
			conn.setReadTimeout(40000 /* milliseconds */);
			conn.setConnectTimeout(10000 /* milliseconds */);
			conn.setRequestMethod("POST");
			in = new BufferedInputStream(conn.getInputStream());

            String mytext = null;
            try {
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();

                myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                myparser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);
                myparser.setInput(in,null);

                boolean stop = false;
                boolean texthere = false;
                int event = myparser.getEventType();

                while (event != XmlPullParser.END_DOCUMENT && !stop) {

                    switch (myparser.getEventType()){
                        case XmlPullParser.START_TAG:
                            if(myparser.getName().equals("success")){
                                texthere = true;
                                success = true;
                                break;
                            }
                            else if(myparser.getName().equals("error")){
                                texthere = true;
                                success = false;
                                break;
                            }
                            break;
                        case XmlPullParser.TEXT:
                            if(texthere){
                                mytext = myparser.getText();
                                stop = true;
                            }
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
            if(in != null){
                in.close();
            }
		}
	}


	private class MyAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute(){
			dialog = new ProgressDialog(getActivity());
            dialog.setTitle(getString(R.string.sending_sms));
            dialog.setMessage(getString(R.string.sending_verification_code_by_SMS));
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

		@Override
        protected void onPostExecute(Void v) {

		//	final Intent intent = new Intent(getActivity(), VerificationCodeActivity.class);

            if (success == true)
			{
                dialog.dismiss();
                //showProgress(false);
				Toast.makeText(getActivity(), textresponse, Toast.LENGTH_LONG).show();
				AssistantActivity.instance().verificateCode(mPhone.getText().toString(), countryCode);
			}
			else
			{   dialog.dismiss();
                //showProgress(false);
				Toast.makeText(getActivity(), textresponse, Toast.LENGTH_LONG).show();
				makingPostRequest = false;

			}
		}

	}
    /*
	public String convertInputStreamToString(InputStream stream, int length) throws IOException, UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[length];
		reader.read(buffer);
		return new String(buffer);
	}
	*/

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
	 * Returns the SHA-512 hashcode of the given string.
	 * @param s the string to be hashed.
	 * @return the SHA-512 hashcode of {@code s}.
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
