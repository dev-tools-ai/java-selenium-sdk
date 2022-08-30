package ai.devtools.utils;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.*;

/**
 * Shared network/http-related utilities and functionality
 */
public class NetUtils
{
	/**
	 * The {@code MediaType} representing the json MIME type.
	 */
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	/**
	 * Performs a simple POST to the specified url with the provided client and {@code RequestBody}.
	 * 
	 * @param client The OkHttp client to use
	 * @param baseURL The base URL to target
	 * @param endpoint The endpoint on the baseURL to target.
	 * @param b The request body to POST.
	 * @return The response from the server, in the form of a {@code Response} object
	 * @throws IOException Network error
	 */
	private static Response basicPOST(OkHttpClient client, HttpUrl baseURL, String endpoint, RequestBody b) throws IOException
	{
		return client.newCall(new Request.Builder().url(baseURL.newBuilder().addPathSegment(endpoint).build()).post(b).build()).execute();
	}

	public static JsonObject post(String url, JsonObject json) throws IOException
	{
		OkHttpClient client = new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(30)).readTimeout(Duration.ofSeconds(30)).build();
		RequestBody payload = new FormBody.Builder().add("json", json.toString()).build();
		Request request = new Request.Builder()
				.url(url)
				.post(payload)
				.build();

		Call call = client.newCall(request);
		try (Response response = client.newCall(request).execute()) {
			return new Gson().fromJson(response.body().string(), JsonObject.class);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Performs a simple POST to the specified url with the provided client and json data.
	 * 
	 * @param client The OkHttp client to use
	 * @param baseURL The base URL to target
	 * @param endpoint The endpoint on the baseURL to target.
	 * @param jo The JsonObject to put in the request body
	 * @return The response from the server, in the form of a {@code Response} object
	 * @throws IOException Network error
	 */
	public static Response basicPOST(OkHttpClient client, HttpUrl baseURL, String endpoint, JsonObject jo) throws IOException
	{
		return basicPOST(client, baseURL, endpoint, RequestBody.create(jo.toString(), JSON));
	}

	/**
	 * Performs a simple form POST to the specified url with the provided client and form data.
	 * 
	 * @param client The OkHttp client to use
	 * @param baseURL The base URL to target
	 * @param endpoint The endpoint on the baseURL to target.
	 * @param form The form data to POST
	 * @return The response from the server, in the form of a {@code Response} object
	 * @throws IOException Network error
	 */
	public static Response basicPOST(OkHttpClient client, HttpUrl baseURL, String endpoint, HashMap<String, String> form) throws IOException
	{
		FormBody.Builder fb = new FormBody.Builder();
		form.forEach(fb::add);

		return basicPOST(client, baseURL, endpoint, fb.build());
	}

	/**
	 * Convenience method, creates a new OkHttpBuilder with timeouts configured.
	 * 
	 * @return A OkHttpClient builder with reasonable timeouts configured.
	 */
	public static OkHttpClient.Builder basicClient()
	{
		Duration d = Duration.ofSeconds(60);
		return new OkHttpClient.Builder().connectTimeout(d).writeTimeout(d).readTimeout(d).callTimeout(d);
	}

	/**
	 * Creates a new {@code OkHttpClient} which ignores expired/invalid ssl certificates. Normally, OkHttp will raise an exception if it encounters bad certificates.
	 * 
	 * @return A new {@code OkHttpClient} which ignores expired/invalid ssl certificates.
	 */
	public static OkHttpClient unsafeClient()
	{
		try
		{
			TrustManager tl[] = { new TrustAllX509Manager() };

			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, tl, new SecureRandom());

			return basicClient().sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tl[0]).hostnameVerifier(new TrustAllHostnameVerifier()).build();
		}
		catch (Throwable e) // highly unlikely, shut up compiler
		{
			return null;
		}
	}

	/**
	 * A dummy {@code HostnameVerifier} which doesn't actually do any hostname checking.
	 * 
	 * @author Alexander Wu (alec@dev-tools.ai)
	 *
	 */
	private static class TrustAllHostnameVerifier implements HostnameVerifier
	{
		@Override
		public boolean verify(String hostname, SSLSession session)
		{
			return true;
		}
	}

	/**
	 * A dummy {@code X509TrustManager} which doesn't actually do any certificate verification.
	 * 
	 * @author Alexander Wu (alec@dev-tools.ai)
	 *
	 */
	private static class TrustAllX509Manager implements X509TrustManager
	{
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}

		@Override
		public X509Certificate[] getAcceptedIssuers()
		{
			return new X509Certificate[0];
		}
	}

}
