//Michael Black, 10/2007
//implements a sender or receiver host, contains the meat of the protocol

//ALL YOUR CODE SHOULD GO INTO THIS CLASS

public class DataLinkHost implements Runnable
{
	//ms before host responds to incoming bytes or new message
	private static final int HOST_LATENCY=1;
	//ms before sender resends
	private static final int TIMEOUT=1000;
	//bytes/frame
	private static final int FRAME_SIZE=4;
	//End-of-frame flag byte
	private static final byte FLAG_BYTE=0x41;
	//escape byte for byte stuffing
	private static final byte ESCAPE_BYTE=0x42;
	//CRC divisor
	private static final byte CRC_KEY=(byte)0x85;
	//header for acknowledgement
	private static final byte ACKHEADER=0x43;
	//header for nonacknowledgement
	private static final byte MSGHEADER=0x44;
	//size of receiver's incoming character buffer
	private static final int RECEIVE_BUFFER_SIZE=10000;

	//the other host
	private DataLinkHost otherHost;
	//the wire leading to the other host
	private Wire wire;

	//entire outgoing message - remove bytes from this and send them
	public String messageToSend="";
	//true while hosts are running
	private boolean isRunning;
	//true if a message is waiting to be sent
	private boolean doSend;
	//true if a message is currently being sent
	private boolean messageInProgress;

	//buffer holding unacknowledged bytes already sent
	private String senderBuffer="";

	//timeout counter
	private int timeout=0;

	//queue of bytes waiting to be received
	private byte[] bytesToReceive;
	//size of queue
	private int bytesWaitingToReceive=0;
	//semaphore protecting queue access; true=queue is being accessed
	private boolean receiveQueueSemaphore=false;
	private byte frameNumber=0;
	private String recivedBuffer="";
	private boolean isEscapeByte=false;
	String lastFrame="";
	
	//host constructor
	//YOU MAY CHOOSE TO ADD TO THIS AS NECESSARY
	public DataLinkHost()
	{
		messageInProgress=false;

		isRunning=true;
		doSend=false;

		bytesToReceive=new byte[RECEIVE_BUFFER_SIZE];
	}

	//YOU SHOULD MODIFY THIS METHOD
	private void sendMessage()
	{
		//YOU SHOULD REPLACE THE FOLLOWING LINES WITH YOUR OWN CODE
		//specifically, you should divide the message into frames and send each frame byte-by-byte
		senderBuffer="";
		senderBuffer+=(char)MSGHEADER;
		
		senderBuffer+=(char)frameNumber;
		int i=0;
		while (messageToSend.length()>0 && i<FRAME_SIZE)
		{
			//send a character from the send buffer
			//wire.sendByte((byte)messageToSend.charAt(0));
			/*if (messageToSend.charAt(0)==FLAG_BYTE)
			{
				senderBuffer+=ESCAPE_BYTE;
				senderBuffer+=messageToSend.charAt(0);
			}else if (messageToSend.charAt(0)==ESCAPE_BYTE)
			{
				senderBuffer+=ESCAPE_BYTE;
				senderBuffer+=messageToSend.charAt(0);
			}else
			{
				senderBuffer+=messageToSend.charAt(0);
			}*/
			senderBuffer+=messageToSend.charAt(0);
			
			//remove the char from the front of the char buffer
			messageToSend=messageToSend.substring(1,messageToSend.length());
			i++;
			
		}
		senderBuffer=computeCRC(senderBuffer);
		
		
		String temp="";
		temp+=senderBuffer.charAt(0);
		temp+=senderBuffer.charAt(1);
		System.out.println(MSGHEADER);
		for (int j=2;j<senderBuffer.length();j++)
		{
			if (senderBuffer.charAt(j)==(char)ESCAPE_BYTE)
			{
				temp+=(char)ESCAPE_BYTE;
				temp+=(char)ESCAPE_BYTE;
			}else if (senderBuffer.charAt(j)==(char)FLAG_BYTE)
				{
					temp+=(char)ESCAPE_BYTE;
					temp+=(char)FLAG_BYTE;
				}else if (senderBuffer.charAt(j)==(char)MSGHEADER)
				{
					temp+=(char)ESCAPE_BYTE;
					System.out.println(MSGHEADER);
					temp+=(char)MSGHEADER;
				}else if (senderBuffer.charAt(j)==(char)ACKHEADER)
				{
					temp+=(char)ESCAPE_BYTE;
					temp+=(char)ACKHEADER;
				}else
				{
					temp+=senderBuffer.charAt(j);
				}
		}
		senderBuffer=new String(temp);
		senderBuffer+=(char)FLAG_BYTE;
		for (int k=0;k<senderBuffer.length();k++)
		{
			wire.sendByte((byte)senderBuffer.charAt(k));
			
		}
		
		timeout=0;
		//System.out.println(senderBuffer);
		
			//if there are more characters, do it again
		
	}

	//this method is executed when a byte comes in
	//MODIFY THIS METHOD
	public void receiveByte(byte b)
	{
		String sendAckMsg="";
		//you should accumulate bytes in a buffer
		//when an end-of-frame flag comes in, assemble the frame
		
		if (b==ESCAPE_BYTE)
		{
			System.out.println("Escape byte");
			isEscapeByte=true;
			return;
		}
		if (isEscapeByte==true)
		{
			isEscapeByte=false;
			//recivedBuffer+=(char)b;
			return;
		}
		
		if (b==FLAG_BYTE)
		{
			System.out.println("inside FLAGBYTE");
			if (recivedBuffer.charAt(0)==MSGHEADER)
			{
				System.out.println("inside MSGHEADER");
				if (recivedBuffer.charAt(1)==frameNumber)
				{
					System.out.println("inside frameNumber");
					if (checkCRC(recivedBuffer))
					{
						System.out.println("inside check CRC");
						System.out.println("data"+recivedBuffer.substring(1, recivedBuffer.length()-1));
						DataLink.messageReceived(""+recivedBuffer.substring(1, recivedBuffer.length()-1));
						if(frameNumber==0)
						{
							frameNumber=1;
						}else
						{
							frameNumber=0;
						}
						sendAckMsg+=(char)ACKHEADER;
						sendAckMsg+=recivedBuffer.charAt(1);
						sendAckMsg=computeCRC(sendAckMsg);
						sendAckMsg+=(char)FLAG_BYTE;
						System.out.println(sendAckMsg);
						for (int j=0;j<sendAckMsg.length();j++)
						{
							wire.sendByte((byte)sendAckMsg.charAt(j));
						}
						sendAckMsg="";
						recivedBuffer="";
					}else
					{
						recivedBuffer="";
						return;
					}
					
				}else if(frameNumber==1 || frameNumber ==0)
				{
					sendAckMsg+=(char)ACKHEADER;
					sendAckMsg+=recivedBuffer.charAt(1);
					sendAckMsg=computeCRC(sendAckMsg);
					sendAckMsg+=(char)FLAG_BYTE;
					for (int j=0;j<sendAckMsg.length();j++)
					{
						wire.sendByte((byte)sendAckMsg.charAt(j));
					}
					recivedBuffer="";
					return;
				}else
				{
					recivedBuffer="";
					return;
				}
			}else if(recivedBuffer.charAt(0)==ACKHEADER)
			{
				if (frameNumber==recivedBuffer.charAt(1))
				{
					if (checkCRC(recivedBuffer))
					{
						System.out.println("inside checkCRC in ACKHEADER");
						if (frameNumber==0)
						{
							frameNumber=1;
						}else
						{
							frameNumber=0;
						}
						sendMessage();
						recivedBuffer="";
					}else
					{
						recivedBuffer="";
						return;
					}
				}else if (recivedBuffer.charAt(1)==1 || recivedBuffer.charAt(1)==0)
				{
					
							
					recivedBuffer="";
					return;
				}else
				{
					recivedBuffer="";
					return;
				}
			}else 
			{
				System.out.println("not Ack and not msgHeader");
				recivedBuffer="";
				return;
			}
			
				
			
		}else
		{
			recivedBuffer+=(char)b;
		}
		
		
		
		//the following line is temporary
		//you shouldn't call messageReceived except with a correct frame
					//remove this
		

	}

	//this method is called when a lot of time has elapsed and the timeout counter hasn't been reset
	//YOU SHOULD MODIFY THIS METHOD
	public void handleTimeout()
	{
		for (int k=0;k<senderBuffer.length();k++)
		{
			wire.sendByte((byte)senderBuffer.charAt(k));
		}
		timeout=0;
	}



	//setLink called after constructors are called, sets up wire to other host
	//YOU SHOULD NOT NEED TO MODIFY THIS
	public void setLink(DataLinkHost host)
	{
		otherHost=host;
		wire=new Wire(otherHost);
	}

	//run is called when the threads start - this is the main loop of the program
	//YOU WILL PROBABLY NOT NEED TO ADD OR MODIFY THIS METHOD (THOUGH YOU CAN IF YOU WANT TO)
	public void run()
	{
		byte toReceive=0;
		boolean doReceive;

		//loop forever
		while(isRunning)
		{
			//if message waiting to be sent, send it
			if (doSend)
			{
				doSend=false;
				sendMessage();
			}
			//if a byte is in the queue waiting to be received, lock the queue and receive it
			while(receiveQueueSemaphore);
			receiveQueueSemaphore=true;
			doReceive=false;
			if (bytesWaitingToReceive>0)
			{
				//take from top of array
				toReceive=bytesToReceive[0];
				doReceive=true;
				//shift array
				for (int i=0; i<bytesWaitingToReceive-1; i++)
					bytesToReceive[i]=bytesToReceive[i+1];
				bytesWaitingToReceive--;
			}
			receiveQueueSemaphore=false;
			//receive the byte from the queue
			if (doReceive)
				receiveByte(toReceive);

			//if a message is currently being sent
			if (messageInProgress)
			{
				//check if everything is sent and stop if the message is done
				if (messageToSend.length()==0)
				{
					messageInProgress=false;
					System.out.println("\nAll sent!");
					DataLink.sendButton.setEnabled(true);
					continue;
				}
				//increment the timeout counter
				timeout++;
				//timeout occured
				if (timeout==TIMEOUT)
				{
					timeout=0;
					handleTimeout();
				}
			}
			//sleep for a millisecond - this makes it real time
			try
			{
				Thread.sleep(HOST_LATENCY);
			}
			catch(Exception e)
			{
				System.out.println("Sleep error");
			}
		}
	}

	//stops the host.  this isn't ever called, but it's here just in case it's needed
	//YOU SHOULD NOT NEED TO MODIFY THIS METHOD.  YOU CAN REMOVE IT IF YOU WANT.
	public void stopHost()
	{
		isRunning=false;
	}

	//called externally when a message is to be sent
	//YOU SHOULD NOT NEED TO MODIFY THIS METHOD
	public void setUpSend(String message)
	{
		messageToSend=message;
		messageInProgress=true;
		doSend=true;
	}

	//called by the wire when a byte comes across
	//YOU SHOULD NOT NEED TO MODIFY THIS METHOD
	public void setUpReceive(byte b)
	{
		//lock the queue and put the byte in it
		while (receiveQueueSemaphore);
		receiveQueueSemaphore=true;
		bytesToReceive[bytesWaitingToReceive]=b;
		bytesWaitingToReceive++;
		receiveQueueSemaphore=false;
	}

	//called with a frame.
	//returns the frame with a CRC added to the end
	//YOU SHOULD NOT NEED TO MODIFY THIS METHOD, BUT YOU SHOULD USE IT
	private String computeCRC(String frame)
	{
		int i=0;

		String subFrame = new String(frame);
		//num holds the number on which we are currently doing long division
		byte num = (byte)subFrame.charAt(0);
		//nextnum holds the next byte.  we will move bits one by one from nextnum into num
		byte nextnum;
		//if the frame has only one byte, nextnum is 0
		if (frame.length()>1)
			nextnum = (byte)subFrame.charAt(1);
		else
			nextnum = (byte)(char)0x00;
		byte remainder=0;

		//remove the bytes in nextnum and num from the frame
		if (frame.length()>1)
			subFrame=subFrame.substring(2,subFrame.length());
		else
			subFrame=subFrame.substring(1,subFrame.length());
		//put 8 zeroes on the end for the remainder (if necessary)
		if (frame.length()>1)
			subFrame=subFrame+(char)0x00;

		//dummy space - keeps the while running one iteration longer
		subFrame=subFrame+" ";

		while(subFrame.length()>0)
		{
			//long division
			if ((0xFF&((int)num))>=(0xFF&((int)CRC_KEY)))
			{
				//quotient term is 1
				remainder=(byte)(num^CRC_KEY);
//				System.out.println("1: num="+num+" rem="+remainder);
			}
			else
			{
				//quotient term is 0
				remainder=(byte)(num^0);
//				System.out.println("0: num="+num+" rem="+remainder);
			}
			//shift num and carry the next bit from nextnum
			num=(byte)(remainder<<1);
			num=(byte)(num+(1&(nextnum>>7)));
			nextnum=(byte)(nextnum<<1);
			i++;
			//nextnum is empty.  replenish it with a byte from frame
			if (i==8)
			{
				nextnum=(byte)subFrame.charAt(0);
				subFrame=subFrame.substring(1,subFrame.length());
				i=0;
//				System.out.println("fetched: "+(byte)nextnum);
			}
		}
		//make remainder a whole byte by putting a 0 at the end
		remainder=(byte)(remainder<<1);

		return frame+(char)remainder;
	}

	//called with a received frame that includes a CRC at the end.
	//returns true if the frame has no errors (the CRC checks out), false if there are errors
	//YOU SHOULD NOT NEED TO MODIFY THIS METHOD BUT YOU SHOULD USE IT
	private boolean checkCRC(String frame)
	{
		int i=0;

		//if the CRC was lost, return false
		if (frame.length()<2)
			return false;

		//num, nextnum are the same as in computeCRC
		String subFrame = new String(frame);
		byte num = (byte)subFrame.charAt(0);
		byte nextnum = (byte)subFrame.charAt(1);
		byte remainder=0;

		subFrame=subFrame.substring(2,subFrame.length());
		//dummy space - keeps the while running longer
		subFrame=subFrame+" ";

		while(subFrame.length()>0)
		{
			//long division
			if ((0xFF&((int)num))>=(0xFF&((int)CRC_KEY)))
			{
				//quotient term is 1
				remainder=(byte)(num^CRC_KEY);
//				System.out.println("1: num="+num+" rem="+remainder);
			}
			else
			{
				//quotient term is 0
				remainder=(byte)(num^0);
//				System.out.println("0: num="+num+" rem="+remainder);
			}
			//shift and carry the next bit
			num=(byte)(remainder<<1);
			num=(byte)(num+(1&(nextnum>>7)));
			nextnum=(byte)(nextnum<<1);
			i++;
			if (i==8)
			{
				nextnum=(byte)subFrame.charAt(0);
				subFrame=subFrame.substring(1,subFrame.length());
				i=0;
			}
		}
		//have to do one more iteration
		if ((0xFF&((int)num))>=(0xFF&((int)CRC_KEY)))
		{
			remainder=(byte)(num^CRC_KEY);
//			System.out.println("1: num="+num+" rem="+remainder);
		}
		else
		{
			remainder=(byte)(num^0);
//			System.out.println("0: num="+num+" rem="+remainder);
		}
		if (remainder==0)
			return true;
		else
			return false;
	}
}
