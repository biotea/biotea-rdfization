package ws.biotea.ld2rdf.rdfGeneration.orcid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class OrcidServiceImpl implements OrcidService {
	private static final String SEARCH_URL = "https://pub.orcid.org/v2.0/search";
	private Logger logger = Logger.getLogger(this.getClass());
	private static OrcidServiceImpl instance;
	
	public static OrcidService getInstance(){
		if (instance == null){
			instance = new OrcidServiceImpl();
		}
		return instance;
	}
	
	private OrcidServiceImpl(){
		super();
	}
	
	
	@Override
	public String getOrcid(String doi, String givenName, String familyName) {
		HttpURLConnection conn = null;
		String orcid = null;
		try {
			String params = this.getParamString(doi, givenName, familyName);
			URL url = new URL(SEARCH_URL + params);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/vnd.orcid+json");

			if (conn.getResponseCode() == 200) {
				BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				String line;
				StringBuilder sb = new StringBuilder();
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				conn.disconnect();
				JSONObject obj = new JSONObject(sb.toString());
				int numResults = obj.getInt("num-found");
				if (numResults == 0) {
					return null;
				}
				orcid = ((JSONObject) obj.getJSONArray("result").get(0)).getJSONObject("orcid-identifier")
						.getString("uri");
				return orcid;
			}
		} catch (Exception e) {
			logger.warn("Error obtaining orcid.", e);
			if (conn != null) {
				conn.disconnect();
			}
		}
		return orcid;
	}

	private String getParamString(String doi, String givenName, String familyName) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		List<String> params = new ArrayList<String>();
		if (givenName != null && !givenName.isEmpty()) {
			params.add(new StringBuilder("given-names:").append(givenName).toString());
		}
		if (familyName != null && !familyName.isEmpty()) {
			params.add(new StringBuilder("family-name:").append(familyName).toString());
		}
		if (doi != null && !doi.isEmpty()) {
			params.add(new StringBuilder("doi-self:").append(doi).toString());
		}
		sb.append("?q=").append(URLEncoder.encode(String.join(" AND ", params), "UTF-8"));

		return sb.toString();
	}

}
