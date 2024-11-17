package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER = "localhost";
    private static final int PORT = 3000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

                System.out.println("Connettiti alla chat: Digita direttamente il messaggio o utilizza i comandi come 'USERS' o 'LOGOUT' \n");
                System.out.println("USERS: per vedere quanti utenti ci sono ");
                System.out.println("LOGOUT: per uscire dalla chat ");
                System.out.println("PRIVATE|username|messaggio: per mandare un messaggio privato \n");

                new Thread(() -> {
                    try {
                        String serverMsg;
                        while ((serverMsg = in.readLine()) != null) {
                            if (serverMsg.equals("ERRORE: Username non trovato")) {
                                System.out.println("Nome utente non trovato. Riprova.");
                                System.out.println("Nome utente non trovato. Riprova.");
                            } else if (serverMsg.equals("ERRORE: Password non trovata")) {
                                System.out.println("Password errata. Riprova.");
                            } else {
                                System.out.println(serverMsg);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();               

            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
                if ("LOGOUT".equalsIgnoreCase(userInput)) break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
