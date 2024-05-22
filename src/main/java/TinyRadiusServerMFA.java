import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusServer;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TinyRadiusServerMFA extends RadiusServer {
	private static final String CONFIG_FILE_PATH = "config.properties";
	
	private Map<String, String> challenges = new HashMap<>();
	private HashMap<String, HashMap<String,String>> users = new HashMap<String, HashMap<String, String>>();
	private String m_secret = "";
	private String m_twilio_from = "";
	
	public TinyRadiusServerMFA() {
		try {
	        // Load configuration from file
	        Properties properties = loadConfiguration(CONFIG_FILE_PATH);
	        String twilioSid = properties.getProperty("twilio.sid");
	        String twilioToken = properties.getProperty("twilio.token");
	        String userName = properties.getProperty("user.name");
	        String userPhone = properties.getProperty("user.phone");
	        String userPassword = properties.getProperty("user.password");

			m_secret = properties.getProperty("radius.secret");
			m_twilio_from = properties.getProperty("twilio.from");

			// user list
			HashMap<String, String> user = new HashMap<String, String>();
			user.put("name", userName);
			user.put("phone", userPhone);
			user.put("password", userPassword);
			users.put(userName, user);
			
			// Initialize Twilio
			Twilio.init(twilioSid, twilioToken);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Properties loadConfiguration(String filePath) throws IOException {
	    Properties properties = new Properties();
	    try (FileInputStream fis = new FileInputStream(filePath)) {
	        properties.load(fis);
	    }
	    return properties;
	}

	@Override
	public String getSharedSecret(InetSocketAddress client) {
		return m_secret;
	}

	@Override
	public String getUserPassword(String userName) {
		if (users.containsKey(userName)) {
			return users.get(userName).get("password");
		}
		
		return null;
	}

	private RadiusPacket accessRequestReceived(AccessRequest request) {
		String userName = request.getUserName();
		String password = request.getUserPassword();

		if (challenges.containsKey(userName)) {
			String expectedCode = challenges.get(userName);
			if (password.equals(expectedCode)) {
				challenges.remove(userName);
				return new RadiusPacket(RadiusPacket.ACCESS_ACCEPT, request.getPacketIdentifier());
			} else {
				return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
			}
		}

		if (password.equals(getUserPassword(userName))) {
			String userPhone = users.get(userName).get("phone");
			if (userPhone != null) {
				String code = generateVerificationCode();
				challenges.put(userName, code);
				sendSms(userPhone, code);
				return new RadiusPacket(RadiusPacket.ACCESS_CHALLENGE, request.getPacketIdentifier());
			}
		}

		return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
	}

	public RadiusPacket accessRequestReceived(AccessRequest request, InetSocketAddress client) {
		return accessRequestReceived(request);
	}

	public RadiusPacket accessRequestReceived(AccessRequest request, InetAddress client) {
		return accessRequestReceived(request);
	}

	private String generateVerificationCode() {
		Random rand = new Random();
		int code = rand.nextInt(900000) + 100000; // Generate a random 6-digit code
		return String.valueOf(code);
	}

	private void sendSms(String to, String code) {
		Message message = Message.creator(new PhoneNumber(to), new PhoneNumber(m_twilio_from), "Your verification code is: " + code).create();
		System.out.println("Sent message to " + to + ": " + message.getBody());
	}

	public static void main(String[] args) {
		TinyRadiusServerMFA server = new TinyRadiusServerMFA();
		try {
			server.start(true, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
