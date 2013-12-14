package com.helpful;

import java.net.*;
import java.io.*;

public class Server {

    private int port;
    private ServerSocket server;
    private Socket connessione;

    public Server (int port)
    {
        this.port = port;
        if(!startServer())
            System.err.println("Errore durante la creazione del Server");
    } // Server()

    private boolean startServer()
    {
        try
        {
            // Crea un server in ascolto sulla porta 'port'
            // Può fallire se la porta è già in uso da un altro programma
            server = new ServerSocket(port);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return false;
        }
        System.out.println("Server creato con successo!");
        return true;

    } // startServer()

    public void runServer()
    {
        while (true)
        {

            try {
                // Il server resta in attesa di una richiesta
                System.out.println("Server in attesa di richieste…");
                connessione = server.accept();
                System.out.println("Un client si e’ connesso…");

                conversazione();
            }
            catch(IOException e) {
                System.out.println("Conversazione interrotta");
            }
        }
    } // runServer()

    public void conversazione() throws IOException
    {
        String sndMessage = "";
        String rcvMessage = "";
        BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
        // Ricava lo stream di input e output dal socket 'connessione'
        // ed utilizza due oggetti wrapper di classe BufferedReader e PrintStream
        // rispettivamente, per semplificare le operazioni di lettura e scrittura
        BufferedReader dalClient = new BufferedReader(new InputStreamReader(connessione.getInputStream()));
        PrintStream alClient = new PrintStream(connessione.getOutputStream());

        //Avvia la conversazione
        alClient.println("Benvenuto!");
        System.out.println("Server: Benvenuto!");
        
        while(!(sndMessage.equals("/logout") || rcvMessage.equals("/logout")))
        {
            rcvMessage = dalClient.readLine();
            System.out.println("Client: " + rcvMessage);
            if(!rcvMessage.equals("/logout"))
            {
                System.out.print("Server: ");
                sndMessage = t.readLine();
                alClient.println(sndMessage);
            }
        } // while
        connessione.close();
        System.out.println("Chiusura connessione effettuata");
    } // conversazione()

    public static void main (String args[])
    {
        // Crea un oggetto di tipo SimpleServer in ascolto
        // sulla porta 'port'
        int port = 7777;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        Server ss = new Server(port);
        // Se la creazione del server è fallita,
        // anche  la seguente istruzione fallirà
        ss.runServer();
    }
}
