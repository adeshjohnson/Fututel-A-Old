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
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.Toast;

import org.linphone.R;
import org.linphone.core.LinphoneAddress.TransportType;

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
public class RegisterFragment extends Fragment implements OnClickListener, TextWatcher {
	private EditText login, password, displayName, domain;
	private RadioGroup transports;
	private Button apply;

	/*Variables agregadas */

	private AutoCompleteTextView mAccountJid;
	private EditText mPassword;
	private EditText mPasswordConfirm;


	private Spinner mCountry;
	private EditText mPhone;
	private String countryCode;
	private EditText mCountryCodeText;
	private ProgressBar mProgressBar;
	/*Botones definidos para GABOTEL */
	private Button mRegisterButton;
	private Button mLoginButton;

	private TableLayout mMoreTable;


	private ImageView mAvatar;


//	private Jid jidToEdit;
//	private Account mAccount;
//	private static final String TAG = RegisterFragment.getSimpleName();
	private boolean mFetchingAvatar = false;
	private String stringToSha1;
	private String sha1CodedString;
	public boolean makingPostRequest = false;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.assistant_register, container, false);

		   /* Definiciones de objetos de acuerdo a elementos de UI */


		this.mCountry = (Spinner) view.findViewById(R.id.country);
		this.mPhone = (EditText) view.findViewById(R.id.phone);
		this.mCountryCodeText = (EditText) view.findViewById(R.id.prefix);
		this.mProgressBar = (ProgressBar) view.findViewById(R.id.login_progress);

		//this.mPasswordConfirm = (EditText) view.findViewById(R.id.account_password_confirm);
		this.mAvatar = (ImageView) view.findViewById(R.id.avater);
		this.mAvatar.setOnClickListener(this.mAvatarClickListener);



		//this.mMoreTable = (TableLayout) view.findViewById(R.id.server_info_more);


		this.mRegisterButton = (Button) view.findViewById(R.id.register_button);
		this.mLoginButton = (Button) view.findViewById(R.id.login_button);

		this.mRegisterButton.setOnClickListener(this.mRegisterButtonClickListener);
		this.mLoginButton.setOnClickListener(this.mLoginButtonClickListener);


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

		mCountryCodeText.setText("+"+countryCode);

		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				Object item = parent.getItemAtPosition(pos);
				countryCode = getResources().getStringArray(R.array.prefixes)[pos];
				mCountryCodeText.setText("+"+countryCode);
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});


		return view;
	}

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

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		apply.setEnabled(!login.getText().toString().isEmpty() && !password.getText().toString().isEmpty() && !domain.getText().toString().isEmpty());
	}

	@Override
	public void afterTextChanged(Editable s) {

	}

	private String downloadContent(String myurl) throws IOException {

        /* Este método realiza la petición POST al servidor de GABOTEL */
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
					.appendQueryParameter("local_number", mPhone.getText().toString())
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
			//Log.d(TAG, "The response is: " + response);
			is = conn.getInputStream();

			// Convert the InputStream into a string
			String contentAsString = convertInputStreamToString(is, length);
			return contentAsString;
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
				showProgress(true);
				return downloadContent(params[0]);

			} catch (IOException e) {
				return "No se pudo establecer la conexión. La URL puede ser invalida";
			}

		}

		protected void onPostExecute(String result){

		//	final Intent intent = new Intent(getActivity(), VerificationCodeActivity.class);

			//Toast para mostrar respuesta xml del servidor
			//Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();

			if (result.contains("success"))
			{
				showProgress(false);
				AssistantActivity.instance().verificateCode(mPhone.getText().toString(), countryCode);
			}
			else
			{
				Toast.makeText(getActivity(), "Revisa el teléfono o código de país", Toast.LENGTH_LONG).show();
				showProgress(false);
				makingPostRequest = false;
			}


		}

		protected void onProgressUpdate(Integer... progress){
			// pb.setProgress(progress[0]);
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

					//Toast para mostrar cadena encriptada
					//Toast.makeText(getActivity(), sha1CodedString, Toast.LENGTH_LONG).show();

					urlForPostRequest = "http://sip.gabotel.com/billing/api/send_sms_verification_code?u=admin&country_prefix="+countryCode+"&local_number="+mPhone.getText().toString()+"&hash="+sha1CodedString;
					//new MyAsyncTask().execute("http://sip.gabotel.com/billing/api/send_sms_verification_code?u=admin&country_prefix=57&local_number=3204837292&hash=ca2496f1b712be2aa275334b538a5c8e398d0cc1");
					makingPostRequest = true;
					//showProgress(true);

					//Ejecuta el proceso asíncrono para la petición post
					new MyAsyncTask().execute(urlForPostRequest);



					//Para pruebas sin solicitar sms
					//final Intent intent = new Intent(getActivity(), VerificationCodeActivity.class);
					//intent.putExtra("phoneNumber",mPhone.getText().toString());
					//intent.putExtra("countryCode", countryCode);
					//startActivity(intent);
					//finish();

				}

			}
		}


	};
	private final OnClickListener mLoginButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(final View v) {

			AssistantActivity.instance().displayLoginGeneric();

		}

	};




	private final OnClickListener mAvatarClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {

		}
	};
}
