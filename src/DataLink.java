//Michael Black, 10/2007
//Simulates a sliding window data link protocol

//YOU SHOULD NOT NEED TO MAKE ANY MODIFICATIONS TO THIS FILE

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DataLink
{
	//graphical objects
	static JFrame sendFrame,recFrame,wireFrame;
	static JTextField sendArea,recArea;
	static JTextArea wireArea;
	static JButton sendButton;

	//the two hosts
	static DataLinkHost sender,receiver;

	public static void main(String[] args)
	{
		//create the frames
		setUpWindows();

		//create the two hosts and set up wires between them
		sender=new DataLinkHost();
		receiver=new DataLinkHost();
		sender.setLink(receiver);
		receiver.setLink(sender);

		//create threads encapsulating the sender and receiver and set them running
		//this makes this application real time
		Thread sendThread,receiveThread;

		sendThread=new Thread(sender);
		receiveThread=new Thread(receiver);
		sendThread.start();
		receiveThread.start();

		//make the windows appear
		sendFrame.setVisible(true);
		recFrame.setVisible(true);
		wireFrame.setVisible(true);
	}

	//sets up the GUI interface
	public static void setUpWindows()
	{
		sendFrame=new JFrame("Sender");
		recFrame=new JFrame("Receiver");
		wireFrame=new JFrame("Wires");
		sendFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		recFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		wireFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sendFrame.setSize(400,120);
		recFrame.setSize(400,120);
		wireFrame.setSize(400,400);

		JPanel sendPanel=new JPanel();
		JPanel recPanel=new JPanel();
		sendButton=new JButton("Send");
		sendButton.addActionListener(new ButtonListener());

		sendArea=new JTextField(30);
		recArea=new JTextField(30);
		wireArea=new JTextArea();
		wireArea.setLineWrap(true);
		JScrollPane wireScroll=new JScrollPane(wireArea);
		sendPanel.add(sendArea);
		sendPanel.add(sendButton);
		sendFrame.add(sendPanel);
		recPanel.add(recArea);
		recFrame.add(recPanel);
		wireFrame.add(wireScroll);

		sendFrame.setLocation(0,0);
		recFrame.setLocation(400,0);
		wireFrame.setLocation(400,200);

		//default message to send
		sendArea.setText("The quick brown fox jumps over the lazy dog");
	}

	//called when a frame is received and processed
	//the frame contents are printed out on the receiver window
	public static void messageReceived(String frameToPrint)
	{
		recArea.setText(recArea.getText()+frameToPrint);
	}

	//when the "Send" button is pressed, start sending the message
	public static class ButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			sendButton.setEnabled(false);
			recArea.setText("");
			sender.setUpSend(sendArea.getText());
		}
	}
}