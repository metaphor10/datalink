//Michael Black, 10/2007
//handles the two wires
//this class sends data between the hosts and scrambles some of it along the way

//YOU ARE NOT PERMITTED TO MODIFY THIS FILE EXCEPT TO CHANGE THE BYTE_ERROR_RATE

public class Wire
{
	//% of time that a byte sent across the wire will be scrambled
	public static final double BYTE_ERROR_RATE=0.2;
	//millisecond delay of wire
	public static final int WIRE_LATENCY=400;

	//receiving host of wire
	private DataLinkHost host;

	//constructor - sets up wire's receiving host
	public Wire(DataLinkHost dlhost)
	{
		host=dlhost;
	}

	//called by host to send byte to the other host
	public void sendByte(byte b)
	{
		if(Math.random()<BYTE_ERROR_RATE)
			b=scramble(b);

		try
		{
			Thread.sleep(WIRE_LATENCY);
		}
		catch(Exception e)
		{
			System.out.println("Sleeping error");
			System.exit(1);
		}

		DataLink.wireArea.setText(DataLink.wireArea.getText()+(byte)b+"("+(char)b+") ");
		host.setUpReceive(b);
	}

	//substitute a random byte for the one sent
	public byte scramble(byte b)
	{
		b=(byte)(Math.random()*256);
		return b;
	}
}
