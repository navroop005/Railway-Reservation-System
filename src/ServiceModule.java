import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class QueryRunner implements Runnable {
    // Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket) {
        this.socketConnection = clientSocket;
    }

    public void run() {
        try {
            // Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                    .getInputStream());
            BufferedReader bufferedInput = new BufferedReader(inputStream);
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                    .getOutputStream());
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream);
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true);
            String clientCommand = "";
            String responseQuery = "";
            // Read client query from the socket endpoint
            clientCommand = bufferedInput.readLine();
            while (!clientCommand.equals("#")) {
                System.out.println("Recieved data <" + clientCommand + "> from client : "
                        + socketConnection.getRemoteSocketAddress().toString());
                clientCommand = clientCommand.trim();
                int length = clientCommand.length();
                int first_n = clientCommand.indexOf(" ");
                String s = clientCommand.substring(0, first_n);
                int num_passengers = Integer.parseInt(s);
                String coach_type = clientCommand.substring(length-2);
                String date = clientCommand.substring(length-13, length -3);
                s = clientCommand.substring(first_n+1, length-14);
                int last_n = s.lastIndexOf(" ");
                String s2 = s.substring(last_n+1);
                int train_id = Integer.parseInt(s2);
                s = s.substring(0, last_n);
                // s = clientCommand.substring(first_n+1);
                StringTokenizer st = new StringTokenizer(s, ",");
                String[] names = new String[num_passengers];
                for (int i = 0; i < num_passengers; i++) {
                    String name = st.nextToken().trim();
                    names[i] = name;
                }

                /*******************************************
                 * Your DB code goes here
                 ********************************************/

                // Dummy response send to client
                responseQuery = "******* Dummy result ******";
                // Sending data back to the client
                printWriter.println(responseQuery);
                // Read next client query
                clientCommand = bufferedInput.readLine();
            }
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

/**
 * Main Class to controll the program flow
 */
public class ServiceModule {
    // Server listens to port
    static int serverPort = 7008;
    // Max no of parallel requests the server can process
    static int numServerCores = 5;

    // ------------ Main----------------------
    public static void start_server() throws IOException {
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        try (// Creating a server socket to listen for clients
                ServerSocket serverSocket = new ServerSocket(serverPort)) {
            Socket socketConnection = null;

            // Always-ON server
            while (true) {
                System.out.println("Listening port : " + serverPort
                        + "\nWaiting for clients...");
                socketConnection = serverSocket.accept(); // Accept a connection from a client
                System.out.println("Accepted client :"
                        + socketConnection.getRemoteSocketAddress().toString()
                        + "\n");
                // Create a runnable task
                Runnable runnableTask = new QueryRunner(socketConnection);
                // Submit task for execution
                executorService.submit(runnableTask);
            }
        }
    }
}
