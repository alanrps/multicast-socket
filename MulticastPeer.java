
/**
 * MulticastPeer: Implementa um peer multicast
 * Descricao: Envia mensagens para todos os membros do grupo.
 */

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.io.Serializable;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MulticastPeer {

	public static void main(String args[]) throws Exception {
		List<String> lista = new ArrayList<String>();

		/* args[0]: ip multicast (entre 224.0.0.0 e 239.255.255.255 */
		MulticastSocket s = null;
		InetAddress group = null;
		int resp = 0;

		try {
			// lista.add("teste");
			// lista.add("teste1");
			// for(String string : lista) {
			// System.out.println(string);
			// }
			/* cria um grupo multicast */
			group = InetAddress.getByName(args[0]);

			/* cria um socket multicast */
			s = new MulticastSocket(6789);

			/* desabilita o recebimento local */
			s.setLoopbackMode(false);

			/* adiciona-se ao grupo */
			s.joinGroup(group);

			/* cria a thread para receber */
			ReceiveThread receiveThread = new ReceiveThread(s);
			receiveThread.start();

			String source = JOptionPane.showInputDialog("Qual o seu nome?");

			byte type = '1';
			String message = "";

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(new Mensagem(type, source, message));
			oos.close();
			oos.flush();

			byte[] obj = baos.toByteArray();
      baos.close();

			s.send(new DatagramPacket(obj, obj.length , group, 6789)); // Instrução indicado o

			do {
				String msg = JOptionPane.showInputDialog("Mensagem?");

				/* cria um datagrama com a msg */
				byte[] m = msg.getBytes();
				DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);

				/* envia o datagrama como multicast */
				s.send(messageOut);

				resp = JOptionPane.showConfirmDialog(null, "Nova mensagem?", "Continuar", JOptionPane.YES_NO_OPTION);
			} while (resp != JOptionPane.NO_OPTION);

			/* retira-se do grupo */
			s.leaveGroup(group);
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (s != null)
				s.close(); // fecha o socket
		} // finally
	} // main
}// class

class ReceiveThread extends Thread {
	MulticastSocket multicastSocket = null;

	public ReceiveThread(MulticastSocket multicastSocket) {
		this.multicastSocket = multicastSocket;
	}

	public void run() {
		try {
			while (true) {
				byte[] buffer = new byte[1000];
				DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
				multicastSocket.receive(messageIn);
				
				ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(messageIn.getData(), messageIn.getOffset(),messageIn.getLength()));
								
				Mensagem asp = (Mensagem) iStream.readObject();
				
				// byte[] bytes = baos.toByteArray();
				// ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				// ObjectInputStream ois = new ObjectInputStream(bais);
				// Mensagem asp = (Mensagem) ois.readObject();

				System.out.println(asp.getType());
				System.out.println(asp.getSource());
				System.out.println(asp.getMessage());

				// System.out.println("Recebido:" + new String(messageIn.getData(), 0, messageIn.getLength()));
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}

class Mensagem implements Serializable {
	private static final long serialVersionUID = 1L;
	private byte type;
	private String source;
	private String message;

	public Mensagem(byte type, String source, String message) {
		this.type = type;
		this.source = source;
		this.message = message;
	}

	public byte getType() {
		return this.type;
	}

	public String getSource() {
		return this.source;
	}

	public String getMessage() {
		return this.message;
	}
}