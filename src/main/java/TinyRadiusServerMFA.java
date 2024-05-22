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
	private Map<String, String> challenges = new HashMap<>();
	private HashMap<String, HashMap<String,String>> users = new HashMap<String, HashMap<String, String>>();
	private String m_secret = "";
	
	public TinyRadiusServerMFA() {
		try {
			System.out.println("Loading data from config.properties...");
			Properties properties = new Properties();
			FileInputStream fis = new FileInputStream("config.properties");
			properties.load(fis);
			fis.close();

			m_secret = properties.getProperty("radius.secret");
			
			// Read properties
			String twilio_sid = properties.getProperty("twilio.sid");
			String twilio_token = properties.getProperty("twilio.token");

			String user_name = properties.getProperty("user.name");
			String user_phone = properties.getProperty("user.phone");
			String user_password = properties.getProperty("user.password");
			HashMap<String, String> user = new HashMap<String, String>();
			user.put("name", user_name);
			user.put("phone", user_phone);
			user.put("password", user_password);
			users.put(user_name, user);
			
			// Initialize Twilio
			Twilio.init(twilio_sid, twilio_token);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getSharedSecret(InetSocketAddress client) {
		return m_secret;
	}

	@Override
	public String getUserPassword(String username) {
		if (users.containsKey(username)) {
			users.get(username).get("password");
		}
		
		return null;
	}

	private RadiusPacket accessRequestReceived(AccessRequest request) {
		String username = request.getUserName();
		String password = request.getUserPassword();

		if (challenges.containsKey(username)) {
			String expectedCode = challenges.get(username);
			if (password.equals(expectedCode)) {
				challenges.remove(username);
				return new RadiusPacket(RadiusPacket.ACCESS_ACCEPT, request.getPacketIdentifier());
			} else {
				return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
			}
		}

		if (password.equals(getUserPassword(username))) {
			String userPhoneNumber = users.get(username).get("password");
			if (userPhoneNumber != null) {
				String code = generateVerificationCode();
				challenges.put(username, code);
				sendSms(userPhoneNumber, code);
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
		Message message = Message.creator(new PhoneNumber(to), new PhoneNumber("+12172161812"),
				"Your verification code is: " + code).create();
		System.out.println("Sent message to " + to + ": " + message.getBody());
	}

	public static void main(String[] args) {
		TinyRadiusServerMFA server = new TinyRadiusServerMFA();
		try {
			server.start(true, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
