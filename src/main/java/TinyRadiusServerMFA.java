import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusServer;

public class TinyRadiusServerMFA extends RadiusServer {
    private static final String CONFIG_FILE_PATH = "config.properties";
    private static final String VERSION = "1.1.0";
    private static final Logger logger = Logger.getLogger(TinyRadiusServerMFA.class.getName());

    private Map<String, String> challenges = new HashMap<>();
    private Map<String, User> users = new HashMap<>();
    private String m_secret = "";
    private boolean m_debug = false;
    
    private TwilioHelper twilioHelper = null;

    public TinyRadiusServerMFA() {
        try {
            logger.info(String.format("Server started (v%s)", VERSION));

            // Load configuration from file
            Properties properties = loadConfiguration(CONFIG_FILE_PATH);
            String twilioSid = properties.getProperty("twilio.sid");
            String twilioToken = properties.getProperty("twilio.token");
            String twilioFrom = properties.getProperty("twilio.from");
            m_secret = properties.getProperty("radius.secret");
            m_debug = "1".equals(properties.getProperty("debug"));

            // Load users
            loadUsers(properties);

            // Initialize Twilio
            twilioHelper = new TwilioHelper(twilioSid, twilioToken, twilioFrom, m_debug);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load configuration", e);
        }
    }

    private void debug(String prefix, Object o) {
        if (!m_debug) return;
        logger.info(String.format("[DEBUG] %s: %s", prefix, o == null ? "null" : o.toString()));
    }

    private Properties loadConfiguration(String filePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
        }
        return properties;
    }

    private void loadUsers(Properties properties) {
        int userIndex = 1;
        while (true) {
            String userName = properties.getProperty("user." + userIndex + ".name");
            if (userName == null) {
                break; // No more users found
            }
            String userPhone = properties.getProperty("user." + userIndex + ".phone");
            String userPassword = properties.getProperty("user." + userIndex + ".password");
            users.put(userName, new User(userName, userPhone, userPassword));
            userIndex++;
        }
        debug("users", users);
    }

    @Override
    public String getSharedSecret(InetSocketAddress client) {
        return m_secret;
    }

    @Override
    public String getUserPassword(String userName) {
        User user = users.get(userName);
        return user != null ? user.getPassword() : null;
    }

    @Override
    public RadiusPacket accessRequestReceived(AccessRequest request, InetSocketAddress client) {
        return handleAccessRequest(request);
    }

    public RadiusPacket accessRequestReceived(AccessRequest request, InetAddress client) {
        return handleAccessRequest(request);
    }

    private RadiusPacket handleAccessRequest(AccessRequest request) {
        String userName = request.getUserName();
        String password = request.getUserPassword();
        debug("userName (client)", userName);
        debug("password (client)", password);
        debug("attributes (client)", request.getAttributes());

        if (password == null) {
            return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
        }

        if (challenges.containsKey(userName)) {
            String expectedCode = challenges.get(userName);
            if (password.equals(expectedCode)) {
                challenges.remove(userName);
                return new RadiusPacket(RadiusPacket.ACCESS_ACCEPT, request.getPacketIdentifier());
            } else {
                return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
            }
        }

        String userPassword = getUserPassword(userName);
        if (userPassword == null) {
            return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
        }

        if (password.equals(userPassword)) {
            String userPhone = users.get(userName).getPhone();
            if (userPhone != null) {
                String code = generateVerificationCode();
                challenges.put(userName, code);
                sendSms(userPhone, code);
                return new RadiusPacket(RadiusPacket.ACCESS_CHALLENGE, request.getPacketIdentifier());
            }
        }

        return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());
    }

    private String generateVerificationCode() {
        Random rand = new Random();
        int code = rand.nextInt(900000) + 100000; // Generate a random 6-digit code
        return String.valueOf(code);
    }

    private void sendSms(String to, String code) {
        twilioHelper.send(to, "Your verification code is: " + code);
    }

    public static void main(String[] args) {
        TinyRadiusServerMFA server = new TinyRadiusServerMFA();
        try {
            server.start(true, false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start server", e);
        }
    }

    private static class User {
        private final String name;
        private final String phone;
        private final String password;

        public User(String name, String phone, String password) {
            this.name = name;
            this.phone = phone;
            this.password = password;
        }

        @SuppressWarnings("unused")
		public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return String.format("User{name='%s', phone='%s'}", name, phone);
        }
    }
}
