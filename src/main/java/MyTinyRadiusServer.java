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

public class MyTinyRadiusServer extends RadiusServer {
	private Map<String, String> challenges = new HashMap<>();
	private Map<String, String> userPhoneNumbers = new HashMap<>();

	public MyTinyRadiusServer() {
		try {
			Properties properties = new Properties();
			FileInputStream fis = new FileInputStream("config.properties");
			properties.load(fis);
			fis.close();

			// Read properties
			String twilio_sid = properties.getProperty("twilio.sid");
			String twilio_token = properties.getProperty("twilio.token");
			String user_name = properties.getProperty("user.name");
			String user_phone = properties.getProperty("user.phone");

			// Initialize Twilio
			Twilio.init(twilio_sid, twilio_token);
			userPhoneNumbers.put(user_name, user_phone);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getSharedSecret(InetSocketAddress client) {
		return "sharedSecret123";
	}

	@Override
	public String getUserPassword(String userName) {
		if (userName.equals("John")) {
			return "token123";
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
			String userPhoneNumber = userPhoneNumbers.get(username);
			if (userPhoneNumber != null) {
				String code = generateVerificationCode();
				challenges.put(username, code);
				sendSms(userPhoneNumber, code);
				return new RadiusPacket(RadiusPacket.ACCESS_CHALLENGE, request.getPacketIdentifier());
			}
		}

		return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
	}

	@Override
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
		System.out.println("Sent message to " + to + ": " + message.getSid());
	}

	public static void main(String[] args) {
		MyTinyRadiusServer server = new MyTinyRadiusServer();
		try {
			server.start(true, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
