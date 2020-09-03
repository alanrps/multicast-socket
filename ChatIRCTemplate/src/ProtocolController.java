
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Properties;

/**
 * Gerencia o protocolo e o processamento das mensagens
 * 
 * @author rodrigo
 */
public class ProtocolController {

    private final MulticastSocket multicastSocket;
    private final DatagramSocket udpSocket;
    private final InetAddress group;
    private final Integer mport, uport;
    private final String nick;
    private final HashMap<String, InetAddress> onlineUsers;
    private final UIControl ui;

    public ProtocolController(Properties properties) throws IOException {
        mport = (Integer) properties.get("multicastPort");
        uport = (Integer) properties.get("udpPort");
        group = (InetAddress) properties.get("multicastIP");
        nick = (String) properties.get("nickname");
        ui = (UIControl) properties.get("UI");

        multicastSocket = new MulticastSocket(mport);
        udpSocket = new DatagramSocket(uport);

        onlineUsers = new HashMap<>();
        onlineUsers.put("Todos", group);
    }
    public byte[] serializacao(Message mensagem) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(mensagem);
        oos.flush();
        oos.close();

        byte[] obj = baos.toByteArray();
        baos.close();

        return obj;
    }

    public Message descerializacao(DatagramPacket messageIn) throws IOException, ClassNotFoundException {
        ObjectInputStream iStream = new ObjectInputStream(
                new ByteArrayInputStream(messageIn.getData(), messageIn.getOffset(), messageIn.getLength()));

        Message objDescerializado = (Message) iStream.readObject();

        return objDescerializado;
    }

    public void send(String targetUser, String msg) throws IOException {
        System.out.println(targetUser);
        if(targetUser.equals("Todos")){
            System.out.println("todos");
            sendMessageGroup(new Message((byte)3, nick, msg));
        }else{
            onlineUsers.containsKey(targetUser);
            InetAddress target = onlineUsers.get(targetUser);
            sendMessage(new Message((byte)4, nick, msg), target);
        }
    }

    private void sendMessageGroup(Message msg) throws IOException {
        byte[] msgBytes = serializacao(msg);
        processPacket(new DatagramPacket(msgBytes, msgBytes.length, group, 6789));
    }

    private void sendMessage(Message msg, InetAddress target) throws IOException {
        byte[] msgBytes = serializacao(msg);
        processPacket(new DatagramPacket(msgBytes, msgBytes.length, target, 6799));
    }   

    public void joinack(String apelido) throws IOException {
        // Pacote para multicast
        Boolean resposta = onlineUsers.containsKey(nick);
        if(resposta != true){
            onlineUsers.put(nick, group);
        }
        byte[] msgBytes = serializacao(new Message((byte)2, apelido, ""));
        processPacket(new DatagramPacket(msgBytes, msgBytes.length, group, 6789));
    }

    public void join() throws IOException {
        multicastSocket.setLoopbackMode(false);
        multicastSocket.joinGroup(group);
        System.out.println("filho das puta");
        Boolean resposta = onlineUsers.containsKey(nick);
        if(resposta != true){
            onlineUsers.put(nick, group);
        }
        byte[] msgBytes = serializacao(new Message((byte)1, nick, ""));
        processPacket(new DatagramPacket(msgBytes, msgBytes.length, group, 6789));
        
    }

    public void leave() throws IOException {
        byte[] msgBytes = serializacao(new Message((byte)5, nick, ""));
        processPacket(new DatagramPacket(msgBytes, msgBytes.length, group, 6789));
    }

    public void close() throws IOException {
        if (udpSocket != null)
            udpSocket.close();
        if (multicastSocket != null)
            multicastSocket.close();
        }

    public void processPacket(DatagramPacket p) throws IOException {
        System.out.println("envio pacote");
        multicastSocket.send(p);
    }

    public void receiveMulticastPacket() throws IOException {
        try {
            System.out.println("recebido");
            byte[] buffer = new byte[1000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length, group, 6789);

            multicastSocket.receive(messageIn);
            
            Message objDescerializado = descerializacao(messageIn);
            System.out.println(objDescerializado.getSource());

            ui.update(objDescerializado);

        } catch (ClassNotFoundException e) {
            System.out.println("erro");
        }
    }
    public void receiveUdpPacket() throws IOException {
        try {
            byte[] buffer = new byte[1000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length, group, 6799);
            udpSocket.receive(messageIn);
            
            Message objDescerializado = descerializacao(messageIn);
            System.out.println(objDescerializado.getSource());
            ui.update(objDescerializado);

        } catch (ClassNotFoundException e) {
            System.out.println("erro");
        }
    }
}
