import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;

public class NotificationThread extends Thread {
	
	static int port_note = 6667, port_frame = 6666;
	static ServerSocket serverSocket_note, serverSocket_frame;
	static Socket socket_note, socket_frame;
	public static OutputStream out_note, out_frame;
	
	public static boolean notify = false;
	public static boolean warn = false;
	
	public NotificationThread(){
		try {
			serverSocket_note = new ServerSocket(port_note);
			serverSocket_frame = new ServerSocket(port_frame);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(String.format("problem2"));
		}
		
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(0, 10000);

			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (notify) {
				System.out.println("video recorded........................blah");
				try {
					socket_note = serverSocket_note.accept();
					out_note = socket_note.getOutputStream();
					out_note.write(1);
					out_note.flush();
					socket_note.close();
					
					socket_frame = serverSocket_frame.accept();
					out_frame = socket_frame.getOutputStream();
					ImageIO.write(Main.sendimg, "jpg", out_frame);
					socket_frame.close();
					// System.out.println(String.format("connected"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println(String.format("connection_prob2"));
					e.printStackTrace();
				}
				notify = false;
			}
			if (warn) {
				System.out.println("alert level 2......................... blah");
				try {
					socket_note = serverSocket_note.accept();
					out_note = socket_note.getOutputStream();
					out_note.write(3);
					out_note.flush();
					socket_note.close();
					
					socket_frame = serverSocket_frame.accept();
					out_frame = socket_frame.getOutputStream();
					ImageIO.write(Main.sendimg, "jpg", out_frame);
					socket_frame.close();
					// System.out.println(String.format("connected"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println(String.format("connection_prob4"));
					e.printStackTrace();
				}
				warn = false;
			}
		}
	}
}
