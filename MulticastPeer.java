/**
 * MulticastPeer: Implementa um peer multicast
 * Descricao: Envia mensagens para todos os membros do grupo.
 */

import java.net.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.io.Serializable;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MulticastPeer {
	public static String source;
	public static byte[] serializacao(Mensagem mensagem) throws IOException{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(mensagem);
			oos.close();
			oos.flush();

			byte[] obj = baos.toByteArray();
			baos.close();

			return obj;
	}

	public static Mensagem descerializacao(DatagramPacket messageIn) throws IOException, ClassNotFoundException{
		ObjectInputStream iStream = new ObjectInputStream(
						new ByteArrayInputStream(messageIn.getData(), messageIn.getOffset(), messageIn.getLength()));

				Mensagem objDescerializado = (Mensagem) iStream.readObject();

				return objDescerializado;
	}

	public static void main(String args[]) throws Exception {
		/* args[0]: ip multicast (entre 224.0.0.0 e 239.255.255.255 */
		MulticastSocket s = null;
		DatagramSocket ds = null;
		InetAddress group = null;
		int resp = 0;

		try {
			/* cria um grupo multicast */
			group = InetAddress.getByName(args[0]);

			String source = args[1];
			MulticastPeer.source = source;
			// JOptionPane.showInputDialog("Qual o seu nome?");
			/* cria um socket multicast */
			s = new MulticastSocket(6789);

			/* desabilita o recebimento local */
			s.setLoopbackMode(false);

			/* adiciona-se ao grupo */
			s.joinGroup(group);

			/* cria a thread para receber */
			ReceiveThread receiveThread = new ReceiveThread(s, group);
			receiveThread.start();

			byte type = (byte) 1;
			String message = "";
			byte[] obj = MulticastPeer.serializacao(new Mensagem(type, source, message, source.length(), message.length()));
			s.send(new DatagramPacket(obj, obj.length, group, 6789));

			do {
				message = JOptionPane.showInputDialog("Mensagem?");
				if(message == null){
					obj = MulticastPeer.serializacao(new Mensagem((byte) 5, source , "", source.length(), 0));

					DatagramPacket mensagemOut = new DatagramPacket(obj,obj.length, group,6789);
					s.send(mensagemOut);

					/* retira-se do grupo */
					s.leaveGroup(group);
				
					return;
				}

				/* cria um datagrama com a msg */
				obj = MulticastPeer.serializacao(new Mensagem((byte) 3, source , message, source.length(), message.length()));
				DatagramPacket messageOut = new DatagramPacket(obj, obj.length, group, 6789);

				/* envia o datagrama como multicast */
				s.send(messageOut);

				resp = JOptionPane.showConfirmDialog(null, "Nova mensagem?", "Continuar", JOptionPane.YES_NO_OPTION);
			} while (resp != JOptionPane.NO_OPTION);

			// Ao digitar que não deseja mais mandar mensagens, usuario sai do grupo
			obj = MulticastPeer.serializacao(new Mensagem((byte) 5, source , "", source.length(), 0));

			DatagramPacket mensagemOut = new DatagramPacket(obj,obj.length, group,6789);
			s.send(mensagemOut);

			/* retira-se do grupo */
			s.leaveGroup(group);
			// receiveThread.interrupt();
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			
			if (s != null)
				s.close();
		} // finally
	} // main
}// class

class ReceiveThread extends Thread {
	MulticastSocket multicastSocket = null;
	InetAddress group;
	boolean flag = false;
	List<String> lista = new ArrayList<String>();

	public ReceiveThread(MulticastSocket multicastSocket, InetAddress group) {
		this.multicastSocket = multicastSocket;
		this.group = group;
	}

	public void run() {
		try {
			while (true) {
				byte[] buffer = new byte[1000];
				DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length, group, 6789);
				multicastSocket.receive(messageIn);

				Mensagem objDescerializado = MulticastPeer.descerializacao(messageIn);
				
				if(objDescerializado.getType() == (byte)1) {
					System.out.println("case one");

							if(flag == false){ 
								MulticastPeer.source = objDescerializado.getSource(); //Adiciona o apelido do usuário em uma variavel global
								flag = true;
							}

							System.out.println("Join: " + objDescerializado.getSource()); //Join de novo usuário
							lista.add(objDescerializado.getSource());
							System.out.println(lista);

								System.out.println("Joinack: " + MulticastPeer.source); //Join de novo usuário
								String message = "";
								byte[] obj = MulticastPeer.serializacao(new Mensagem((byte)2, MulticastPeer.source , message, MulticastPeer.source.length(), message.length()));
								DatagramPacket messageOut = new DatagramPacket(obj, obj.length, group, 6789);
								multicastSocket.send(messageOut);
						}
				
				if(objDescerializado.getType() == (byte)2) {
					Boolean verificacao = false;
					System.out.println("case two");
					// System.out.println("objeto "+objDescerializado.getSource());
					// System.out.println("meu nome " +MulticastPeer.source);
					for (String item : lista) {
							if(objDescerializado.getSource().equals(item) == true){
								verificacao = true;
							}	
					}
					if(verificacao != true){
						lista.add(objDescerializado.getSource());
					}
					System.out.println(lista);
				}
				if(objDescerializado.getType() == (byte)3){
					System.out.println("MSG " + objDescerializado.getSource()+ " " + objDescerializado.getMessage());
				}
				if(objDescerializado.getType() == (byte)5){
					System.out.println("LEAVE " + objDescerializado.getSource());
					lista.remove(objDescerializado.getSource());
					System.out.println(lista);
				}
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
	private int size_source;
	private int size_message;

	public Mensagem(byte type, String source, String message, int size_source, int size_message) {
		this.type = type;
		this.source = source;
		this.message = message;
		this.size_source = size_source;
		this.size_message = size_message;
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

	public int getSizeSource() {
		return this.size_source;
	}

	public int getSizeMessage() {
		return this.size_message;
	}
}