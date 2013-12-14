package com.helpful;

import java.net.*;
import java.io.*;

public class Client
{
    private Socket connessione;

    public Client(String address, int port)
    {
        try
        {
            // Apre una connessione verso un server in ascolto
            // sulla porta 7777. In questo caso utilizziamo localhost
            // che corrisponde all’indirizzo IP 127.0.0.1
            System.out.println("Apertura connessione…");
            connessione = new Socket (address, port);
        }
        catch (ConnectException connExc)
        {
            System.err.println("Errore nella connessione ");
        }
        catch(IOException e) {
            System.err.println("Errore nella connessione ");
        }
    }

    public void conversazione()
    {
        try
        {
            String sndMessage = "";
            String rcvMessage = "";
            BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
            // Ricava lo stream di input e output dal socket 'connessione'
            // ed utilizza due oggetti wrapper di classe BufferedReader e PrintStream
            // rispettivamente, per semplificare le operazioni di lettura e scrittura
            BufferedReader dalServer = new BufferedReader(new InputStreamReader(connessione.getInputStream()));
            PrintStream alServer = new PrintStream(connessione.getOutputStream());

            while(!(sndMessage.equals("/logout") || rcvMessage.equals("/logout")))
            {
                rcvMessage = dalServer.readLine();
                System.out.println("Server: " + rcvMessage);
                if(!rcvMessage.equals("/logout"))
                {
                    System.out.print("Client: ");
                    sndMessage = t.readLine();
                    alServer.println(sndMessage);
                }
            } // while
            connessione.close();
            System.out.println("Chiusura connessione effettuata");
        }
        catch(IOException e) {
            System.out.println("Conversazione interrotta");
        }
    } // conversazione()

    public static void main(String args[])
    {
        String address = "127.0.0.1";
        int port = 7777;
        if (args.length > 0)
            address = args[0];
        if (args.length > 1)
            port = Integer.parseInt(args[1]);
        // Crea un oggetto Client che tenta una connessione
        // verso un server con indirizzo 'address' sulla porta 'port'
        Client cl = new Client(address, port);
        // Se la connessione al server è fallita,
        // anche  la seguente istruzione fallirà
        cl.conversazione();
    }
}