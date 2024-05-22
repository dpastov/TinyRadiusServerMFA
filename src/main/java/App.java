public class App {
	public static void main(String[] args) {
		MyTinyRadiusServer server = new MyTinyRadiusServer();
		server.start(true, true); // Listen on both auth and acct ports
	}
}
