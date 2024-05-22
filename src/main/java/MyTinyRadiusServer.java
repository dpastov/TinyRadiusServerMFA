import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusServer;

public class MyTinyRadiusServer extends RadiusServer {

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

		if (password.equals(getUserPassword(username))) {
			RadiusPacket accessAcceptPacket = new RadiusPacket(RadiusPacket.ACCESS_ACCEPT, request.getPacketIdentifier());
			return accessAcceptPacket;
		}

		return new RadiusPacket(RadiusPacket.ACCESS_REJECT, request.getPacketIdentifier());	
	}

	public RadiusPacket accessRequestReceived(AccessRequest request, InetSocketAddress client) {
		return accessRequestReceived(request);
	}

	public RadiusPacket accessRequestReceived(AccessRequest request, InetAddress client) {
		return accessRequestReceived(request);
	}
}
