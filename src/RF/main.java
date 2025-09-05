package RF;

public class main {

	public static void main(String[] args) {

		cMAC MAC = new cMAC("COM5");
		ARP arp = new ARP(MAC);
		Inverters invs =  new Inverters(MAC);
		
		MAC.registerHandler(0x01, arp);
		MAC.registerHandler(0x02, invs);

	}

}
