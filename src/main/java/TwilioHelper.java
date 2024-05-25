import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwilioHelper {
    private static final Logger logger = Logger.getLogger(TwilioHelper.class.getName());
    private static final String BASE_API = "https://api.twilio.com/2010-04-01";

    private String m_account_sid;
    private String m_auth_token;
    private String m_phone;
    private boolean debug;

    public TwilioHelper(String account_sid, String auth_token, String twilio_phone, boolean debug) {
        this.m_account_sid = account_sid;
        this.m_auth_token = auth_token;
        this.m_phone = twilio_phone;
        this.debug = debug;
    }

    private void debug(String message) {
        if (debug) {
            logger.info(message);
        }
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    public int send(String phoneTo, String body) {
        debug("twilio.send: started");

        int res = 0;
        HttpURLConnection conn = null;
        try {
            URI uri = new URI(BASE_API + "/Accounts/" + m_account_sid + "/Messages.json");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String data = "To=" + encode(phoneTo) + "&From=" + encode(m_phone) + "&Body=" + encode(body);
            byte[] out = data.getBytes(StandardCharsets.UTF_8);
            int length = out.length;
            String userCredentials = m_account_sid + ":" + m_auth_token;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes(StandardCharsets.UTF_8));

            conn.setFixedLengthStreamingMode(length);
            conn.addRequestProperty("Authorization", basicAuth);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            res = conn.getResponseCode();

            if (res >= 200 && res <= 299) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    debug("> response: " + response.toString());
                }
            } else {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    debug("> error response: " + response.toString());
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send SMS", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        debug("> response code: " + res);

        debug("twilio.send: completed");
        return res;
    }
}
