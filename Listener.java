import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Thread que gerencia threads que processam recebimento de mensagens
 *
 * @author Rodrigo Campiolo
 */
public class Listener extends Thread {

    Thread multicastListener, udpListener;
    ProtocolController protoController;

    /* Construtor */
    public Listener(ProtocolController protoController) {
        this.protoController = protoController;
    } // construtor

    /* execucao da thread */
    public void run() {
        /* thread para rebecer pacotes via multicast */
        multicastListener = new Thread(() -> {
            try {
                while (true) {
                    protoController.receiveMulticastPacket();
                }
            } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
                System.out.println(ex);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            }
        });

        /* thread para receber pacotes via UDP */
        udpListener = new Thread(() -> {
            try {
                while (true) {
                    protoController.receiveUdpPacket();
                }
            } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
                System.out.println(ex);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            }

        });

        /* inicialiar as threads */
        multicastListener.start();
        udpListener.start();

        try {
            Thread.sleep(1000);
            // aqui poderia ser implementada uma função de observação das threads
        } catch (InterruptedException ie) {
            System.out.println(ie);
        }
    } //run
} // class ClientListener

