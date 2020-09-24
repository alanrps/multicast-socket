import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Properties;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gerencia o protocolo e o processamento das mensagens
 * 
 * @author Rodrigo e Alan Patriarca
 */

public class ProtocolController {

    private MulticastSocket multicastSocket;
    private DatagramSocket udpSocket;
    private InetAddress group;
    private Integer mport, uport;
    private String nick;
    private HashMap<String, InetAddress> onlineUsers;
    private UIControl ui;
    private static String chave = "campiolo";
    private static SecretKeySpec secretKey;
    private static byte[] key;

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
        setKey(chave); 
    }

    // Métodos utilizados para criptografia 

    /**
    * Faz a configuração da secretKey
    * @param chave - Valor da chave definida
    */
    public static void setKey(String chave) { 
        MessageDigest sha = null;
        try {
            key = chave.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
    * Criptografa os dados utilizando o algoritmo AES
    * @param messageEncrypt - Valor em bytes da mensagem a ser criptografada
    * @return byte[] - Mensagem criptografada
    */
    public static byte[] encrypt(byte[] messageEncrypt) { 
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            return cipher.doFinal(messageEncrypt);
            // return Base64.getEncoder().encode(messageEncrypt);
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    /**
    * Descriptografa os dados
    * @param strToDecrypt - Valor em bytes da mensagem a ser descriptografada
    * @return byte[] - Mensagem descriptografada
    */
    public static byte[] decrypt(byte[] strToDecrypt) { 
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            return cipher.doFinal(strToDecrypt);
            // return Base64.getDecoder().decode(strToDecrypt);

        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
    
    //--------------------------------------------------------------------------

    /**
    * Método utilizado para que dada uma string, se converta em inteiro, Exemplo String "12" --> int 12
    * @param str - String com o tamanho do pacote criptografado
    * @return int - retorna o tamanho 
    */
    public int atoi(String str) {
        String target = str.trim();
        int base = 0;
        int sign = 1;
        boolean overflow = false;
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            if ((i == 0) && (c == '-' || c == '+')) {
                sign = (c == '-') ? -1 : 1;
                continue;
            }

            if (c >= '0' && c <= '9') {
                if (base > 100000000) {
                    double tmp = base * 10.0 + (double) (c - '0');
                    if (tmp > Integer.MAX_VALUE) {
                        overflow = true;
                        break;
                    }
                }
                base = base * 10 + (int) (c - '0');
            } else {
                break;
            }
        }

        if (overflow)
            return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        return base * sign;
    }

    // Métodos utilizados para fazer o fluxo do chat sem criptografia

    /**
    * Método utilizado para fazer a seleção se a mensagem é para ser enviada via Multicast, ou udp, ou seja
    se o destino é o grupo todo ou apenas um usuário específico
    * @param targetUser - String com o destinatário
    * @param msg - String com a mensagem
    */
    public void send(String targetUser, String msg) throws IOException {
        if (targetUser.equals("Todos")) {
            sendMessageGroup(new Message((byte) 3, nick, msg));
        } else {
            InetAddress target = onlineUsers.get(targetUser);
            sendMessage(new Message((byte) 4, nick, msg), target);
        }
    }

    /**
    * Método utilizado para enviar uma mensagem via multicast
    * @param msg - Objeto do tipo Message(type, source, message) 
    */
    private void sendMessageGroup(Message msg) throws IOException {
        byte[] msgBytes = msg.getBytes();
        multicastSocket.send(new DatagramPacket(msgBytes, msgBytes.length, group, mport)); 
    }

    /**
    * Método utilizado para enviar uma mensagem via udp
    * @param msg - Objeto do tipo Message(type, source, message) 
    * @param target - Endereço ip do destinatário
    */
    private void sendMessage(Message msg, InetAddress target) throws IOException {
        byte[] msgBytes = msg.getBytes();
        udpSocket.send(new DatagramPacket(msgBytes, msgBytes.length, target, uport));
    }

    /**
    * Método utilizado para fazer a manutenção da lista de usuários ativos
    * @param apelido - nome de quem entrou no grupo, ou seja, fez um JOIN
    */
    public void joinack(String apelido) throws IOException {
        Message msg = new Message((byte) 2, nick, "");
        sendMessage(msg, onlineUsers.get(apelido));
    }

    /**
    * Método utilizado para entrar no grupo
    */
    public void join() throws IOException {
        multicastSocket.joinGroup(group);
        sendMessageGroup(new Message((byte) 1, nick, ""));
    }

    /**
    * Método utilizado para deixar o grupo
    */
    public void leave() throws IOException {
        sendMessageGroup(new Message((byte) 5, nick, ""));
    }

    /**
    * Método utilizado para fazer o processamento do pacote recebido
    * @param p - Pacote contendo os dados da mensagem recebida  
    */
    public void processPacket(DatagramPacket p) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Message message = new Message(p.getData());
        String fonteNick = message.getSource(); // Consegue o apelido da fonte da mensagem

        if (message.getType() == 1) {
            if (nick.equals(fonteNick) == false) { // Salva o apelido e endereço ip na lista e faz joinack
                onlineUsers.put(fonteNick, p.getAddress());
                joinack(fonteNick);
            }
        } else if (message.getType() == 2) { // Salva apelido e endereço ip na lista
            onlineUsers.put(fonteNick, p.getAddress());
        } else if (message.getType() == 5) { // remove da lista de usuários
            onlineUsers.remove(fonteNick);
        }

        ui.update(message);
    }

    //--------------------------------------------------------------------------

    //Métodos utilizados para fazer o fluxo do chat com criptografia

    /**
    * Método utilizado para entrar no grupo --> criptografado
    */
    public void joinCriptografado() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        multicastSocket.joinGroup(group); // Entra no grupo
        sendMessageGroupCriptografado(new Message((byte) 1, nick, "")); // Envia message do tipo "JOIN"
    }

    /**
    * Método utilizado para fazer a manutenção da lista de usuários ativos --> criptografado
    * @param apelido - nome de quem entrou no grupo, ou seja, fez um JOIN
    */
    public void joinackCriptografado(String apelido) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        sendMessageCriptografado(new Message((byte) 2, nick, ""), apelido);
    }

     /**
    * Método utilizado para enviar o pacote inicial indicando se a mensagem será criptografada ou não,
    via multicast ou udp --> criptografado
    * @param target - Destinatário da mensagem
    * @param target - Tamanho da mensagem, utilizado para receber a mensagem criptografada
    */
    public void pacoteInicial(String target, int tamanhoMensagem) throws IOException {
        try {
            Message msg;
            byte[] msgBytes;
            if (target.equals("Todos")) { // Envia como multicast
                msg = new Message((byte) -1, nick, Integer.toString(tamanhoMensagem));
                msgBytes = msg.getBytes();
                multicastSocket.send(new DatagramPacket(msgBytes, msgBytes.length, group, mport));

            } else { // Envia como UDP
                msg = new Message((byte) -1, nick, Integer.toString(tamanhoMensagem));
                msgBytes = msg.getBytes();
                udpSocket.send(new DatagramPacket(msgBytes, msgBytes.length, onlineUsers.get(target), uport));
            }
        } catch (NumberFormatException e) {
            System.out.println(e);
        }
    }

     /**
    * Método utilizado para fazer a seleção se a mensagem é para ser enviada via Multicast, ou udp, ou seja
    se o destino é o grupo todo ou apenas um usuário específico --> criptografado
    * @param targetUser - String com o destinatário
    * @param msg - String com a mensagem
    */
    public void sendCriptografado(String targetUser, String msg) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException {
        if (targetUser.equals("Todos")) { // Compara o destinatário da mensagem, neste caso é quando a mensagem é
            sendMessageGroupCriptografado(new Message((byte) 3, nick, msg));
        } else { // Caso a mensagem possua um destinatário unico, é enviado via udp
            sendMessageCriptografado(new Message((byte) 4, nick, msg), targetUser);
        }
    }

    /**
    * Método utilizado para enviar uma mensagem via multicast --> criptografado
    * @param msg - Objeto do tipo Message(type, source, message)
    */
    private void sendMessageGroupCriptografado(Message msg) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NumberFormatException {
        byte[] msgBytes = encrypt(msg.getBytes());
        pacoteInicial("Todos", msgBytes.length);
        multicastSocket.send(new DatagramPacket(msgBytes, msgBytes.length, group, mport));
    }

    /**
    * Método utilizado para enviar uma mensagem via udp
    * @param msg - Objeto do tipo Message(type, source, message) 
    * @param target - String com o destinatário
    */
    private void sendMessageCriptografado(Message msg, String target) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        byte[] msgBytes = encrypt(msg.getBytes());
        pacoteInicial(target, msgBytes.length);
        udpSocket.send(new DatagramPacket(msgBytes, msgBytes.length, onlineUsers.get(target), uport));
    }

    /**
    * Método utilizado para deixar o grupo --> criptografado
    */
    public void leaveCriptografado() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        sendMessageGroupCriptografado(new Message((byte) 5, nick, ""));
    }

    /**
    * Método utilizado para fazer o processamento do pacote recebido --> criptografado
    * @param p - Pacote contendo os dados da mensagem recebida  
    */
    public void processPacketCriptografado(DatagramPacket p) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        byte[] decryptedString = decrypt(p.getData());
        Message message = new Message(decryptedString);
        String fonteNick = message.getSource(); // Consegue o apelido da fonte da mensagem

        if (message.getType() == 1) {
            if (nick.equals(fonteNick) == false) { // Salva o apelido e endereço ip na lista e faz joinack
                onlineUsers.put(fonteNick, p.getAddress());
                joinackCriptografado(fonteNick);
            }
        } else if (message.getType() == 2) { // Salva apelido e endereço ip na lista
            onlineUsers.put(fonteNick, p.getAddress());
        } else if (message.getType() == 5) { // remove da lista de usuários
            onlineUsers.remove(fonteNick);
        }
        ui.update(message);
    }

    //--------------------------------------------------------------------------

    // Métodos utilizados em ambos os fluxos

    /**
    * Método utilizado para finalizar a conexão, isto é, finaliza a conexão com o socket udp e tcp
    */
    public void close() throws IOException {
        if (udpSocket != null)
            udpSocket.close();
        if (multicastSocket != null)
            multicastSocket.close();
    }

    /**
    * Método utilizado para fazer o recebimento dos pacotes via multicast, recebe tanto mensagens criptografadas,
    quanto descriptografadas
    */
    public void receiveMulticastPacket() throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        byte[] buffer = new byte[1000];
        int tamanho = 0;
        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length, group, mport);
        multicastSocket.receive(messageIn);

        Message message = new Message(messageIn.getData()); // Descerializar o objeto

        byte[] bufferDadosCriptografados;
        if (Byte.compare(message.getType(), (byte) -1) == 0) { // Criptografada
            tamanho = atoi(message.getMessage());
            bufferDadosCriptografados = new byte[tamanho];
            messageIn = new DatagramPacket(bufferDadosCriptografados, bufferDadosCriptografados.length, group, mport);
            multicastSocket.receive(messageIn);

            processPacketCriptografado(messageIn);

        } else { // Não criptografada
            processPacket(messageIn);
        }
    }

    /**
    * Método utilizado para fazer o recebimento dos pacotes via udp, recebe tanto mensagens criptografadas,
    quanto descriptografadas
    */
    public void receiveUdpPacket() throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        int tamanho = 0;
        byte[] buffer = new byte[1000];
        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length, group, uport);
        udpSocket.receive(messageIn);
        Message message = new Message(messageIn.getData()); // Descerializar o objeto

        if (Byte.compare(message.getType(), (byte) -1) == 0) { // Criptografada
            tamanho = atoi(message.getMessage());
            buffer = new byte[tamanho];
            messageIn = new DatagramPacket(buffer, buffer.length, group, uport);
            udpSocket.receive(messageIn);

            processPacketCriptografado(messageIn);
        } else { // Não criptografada
            processPacket(messageIn);
        }
    }
}

