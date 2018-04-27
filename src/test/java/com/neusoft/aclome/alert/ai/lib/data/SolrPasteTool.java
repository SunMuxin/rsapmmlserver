package com.neusoft.aclome.alert.ai.lib.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SolrPasteTool {
	
	private static final String COMMIT_XML = "<commit/>";
	protected URL solrUrl;

	/**
	 * Post all filenames provided in args, return the number of files posted
	 * 
	 * @throws Exception
	 */
	int postFiles(String[] args, int startIndexInArgs, OutputStream out,
			String dataType) throws Exception {
		int filesPosted = 0;
		for (int j = startIndexInArgs; j < args.length; j++) {
			File srcFile = new File(args[j]);
			if (srcFile.canRead()) {
				postFile(srcFile, out, dataType);
				filesPosted++;
			}
		}
		return filesPosted;
	}

	/**
	 * Constructs an instance for posting data to the specified Solr URL (ie:
	 * "http://localhost:8983/solr/update")
	 */
	public SolrPasteTool(URL solrUrl) {
		this.solrUrl = solrUrl;
	}

	/**
	 * Does a simple commit operation TODO,json
	 * 
	 * @throws Exception
	 */
	public void commit() throws Exception {
		postDocs(COMMIT_XML, "application/xml");
	}

	public String postDocs(String docs, String dataType) throws Exception {
		InputStream is = null;
		try {
			byte[] data = docs.getBytes("UTF-8");
			is = new ByteArrayInputStream(data);
			return postData(is, data.length, dataType);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Opens the file and posts it's contents to the solrUrl, writes to response
	 * to output.
	 * 
	 * @throws Exception
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public void postFile(File file, OutputStream output, String dataType)
			throws Exception {

		InputStream is = null;
		try {
			is = new FileInputStream(file);
			postData(is, (int) file.length(), dataType);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads data from the data stream and posts it to solr, writes to the
	 * response to output
	 * 
	 * @throws Exception
	 */
	public String postData(InputStream data, Integer length, String dataType)
			throws Exception {

		HttpURLConnection urlc = null;
		try {
			try {
				urlc = (HttpURLConnection) solrUrl.openConnection();
				try {
					urlc.setRequestMethod("POST");
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				urlc.setDoOutput(true);
				urlc.setDoInput(true);
				urlc.setUseCaches(false);
				urlc.setAllowUserInteraction(false);
				urlc.setRequestProperty("Content-Type", dataType);
				urlc.setRequestProperty("charset", "UTF-8");

				if (null != length)
					urlc.setFixedLengthStreamingMode(length);

			} catch (IOException e) {
				throw new Exception("Connection error (is Solr running at "
						+ solrUrl + " ?)", e);

			}

			OutputStream out = null;
			try {
				out = urlc.getOutputStream();
				pipe(data, out);
			} catch (IOException e) {
				throw new Exception("IOException while posting data", e);
			} finally {
				try {
					if (out != null)
						out.close();
				} catch (IOException x) { /* NOOP */
				}
			}

			InputStream in = null;
			try {
				if (HttpURLConnection.HTTP_OK != urlc.getResponseCode()) {
					throw new Exception("Solr returned an error #"
							+ urlc.getResponseCode() + " "
							+ urlc.getResponseMessage());
				}
				// to do ,response
				in = urlc.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				pipe(in, output);
				return output.toString("UTF-8");
			} catch (IOException e) {
				throw new Exception("IOException while reading response: ", e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException x) { /* NOOP */
				}
			}

		} finally {
			 if (urlc != null)
			 urlc.disconnect();
		}
	}

	/**
	 * Pipes everything from the source to the dest. If dest is null, then
	 * everything is read fro msource and thrown away.
	 */
	private static void pipe(InputStream source, OutputStream dest)
			throws IOException {
		byte[] buf = new byte[1024];
		int read = 0;
		while ((read = source.read(buf)) >= 0) {
			if (null != dest)
				dest.write(buf, 0, read);
		}
		if (null != dest)
			dest.flush();
	}
	
	public static void main(String[] args) throws Exception {
		SolrPasteTool spt = new SolrPasteTool(new URL("http://10.0.67.21:8080/solr/nifi/update"));
		while(true) {
			JsonArray docs = new JsonArray();
			for (int i = 0; i < 2000; i++) {
				JsonObject doc = new JsonObject();
				doc.addProperty("title", "Doc 2");
				doc.addProperty("id", UUID.randomUUID().toString());
				docs.add(doc);
			}
			spt.postDocs(docs.toString(), "application/json");
			spt.commit();
		}
	}

}
