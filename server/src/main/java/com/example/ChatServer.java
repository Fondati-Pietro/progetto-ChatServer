package com.example;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 3000;
    private static Map<String, GestioneClient> clients = Collections.synchronizedMap(new HashMap<>());
    static Map<String, String> userDatabase = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Il server ha aperto la porta: " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuovo utente connesso: " + socket.getInetAddress());

                GestioneClient gestore = new GestioneClient(socket);
                new Thread(gestore).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized boolean addUser(String username, GestioneClient handler) {
        if (clients.containsKey(username)) return false;
        clients.put(username, handler);
        return true;
    }

    static synchronized void removeUser(String username) {
        clients.remove(username);
    }

    static synchronized void broadcast(String message, String sender) throws IOException {
        for (Map.Entry<String, GestioneClient> entry : clients.entrySet()) {
            if (!entry.getKey().equals(sender)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    static synchronized List<String> getOnlineUsers() {
        return new ArrayList<>(clients.keySet());
    }

    static synchronized GestioneClient getClient(String username) {
        return clients.get(username);
    }
}

class GestioneClient implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private DataOutputStream out;
    private String username;

    public GestioneClient(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());

            gestioneLogin_Signup();

            String message;
            while ((message = in.readLine()) != null) {
                comandi(message);
            }
        } catch (IOException e) {
            System.out.println("Errore di connessione con " + username);
        } finally {
            try {
                pulizia();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void pulizia() throws IOException {
        if (username != null) {
            ChatServer.removeUser(username);
            ChatServer.broadcast("NOTIFICA: " + username + " è uscito", null);
            System.out.println("User " + username + " è uscito.");
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Errore di chiusura per l'utente " + username);
            e.printStackTrace();
        }
    }

    private void gestioneLogin_Signup() {
        boolean autentificazione = false;

        while (!autentificazione) {
            try {
                sendMessage("Scegli: 'L' per LOGIN o 'S' per SIGNUP");
                String choice = in.readLine();
                if ("L".equalsIgnoreCase(choice)) {
                    sendMessage("Inserisci username:");
                    String user = in.readLine();
                    sendMessage("Inserisci password:");
                    String pass = in.readLine();

                    if (!ChatServer.userDatabase.containsKey(user)) {
                        sendMessage("ERRORE: Username non trovato");
                    } else if (!ChatServer.userDatabase.get(user).equals(pass)) {
                        sendMessage("ERRORE: Password non valida");
                    } else {
                        username = user;
                        ChatServer.addUser(username, this);
                        sendMessage("SUCCESS|LOGIN|" + username);
                        ChatServer.broadcast("NOTIFICA: " + username + " si è unito", username);
                        autentificazione = true;
                    }
                } else if ("S".equalsIgnoreCase(choice)) {
                    sendMessage("Inserisci un nuovo username:");
                    String user = in.readLine();

                    if (ChatServer.userDatabase.containsKey(user)) {
                        sendMessage("ERRORE: Username già in uso");
                    } else {
                        sendMessage("Inserisci una password:");
                        String pass = in.readLine();
                        ChatServer.userDatabase.put(user, pass);
                        username = user;
                        ChatServer.addUser(username, this);
                        sendMessage("SUCCESS|SIGNUP|" + username);
                        ChatServer.broadcast("NOTIFICA: " + username + " si è unito", username);
                        autentificazione = true;
                    }
                } else {
                    sendMessage("Scelta invalida.");
                }
            } catch (IOException e) {
                System.out.println("Errore durante login/signup: " + e.getMessage());
            }
        }
    }

    private void comandi(String message) throws IOException {
        String[] parts = message.split("\\|");
        String command = parts[0];

        switch (command) {
            case "USERS":
                sendMessage("USERS|" + String.join(",", ChatServer.getOnlineUsers()));
                break;

            case "LOGOUT":
                sendMessage("SUCCESS|LOGOUT|" + username);
                ChatServer.removeUser(username);
                ChatServer.broadcast("NOTIFICA: " + username + " è uscito", username);
                break;

            case "PRIVATE":
                if (parts.length >= 3) {
                    String recipient = parts[1];
                    String privateMessage = parts[2];
                    GestioneClient recipientClient = ChatServer.getClient(recipient);

                    if (recipientClient != null) {
                        recipientClient.sendMessage("PRIVATE|" + username + "|" + privateMessage);
                    } else {
                        sendMessage("ERRORE: Utente non trovato o offline");
                    }
                } else {
                    sendMessage("ERRORE: comando PRIVATE non valido");
                }
                break;

            default:
                ChatServer.broadcast("MESSAGGIO DA: |" + username + "| " + message, username);
                break;
        }
    }

    public void sendMessage(String message) throws IOException {
        out.writeBytes(message + "\n");
    }
}