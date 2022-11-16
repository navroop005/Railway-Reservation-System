import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class QueryRunner implements Runnable {
    // Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket) {
        this.socketConnection = clientSocket;
    }

    static int berths_per_ac = 18;
    static int berths_per_sl = 24;

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
                boolean isCorrect = true;
                System.out.println("Recieved data <" + clientCommand + "> from client : "
                        + socketConnection.getRemoteSocketAddress().toString());

                int train_id = 0;
                String doj_str = "";
                int num_passengers = 0;
                LocalDate doj = LocalDate.MIN;
                String coach_type = "";
                String[] passenger_names = new String[0];
                try {
                    clientCommand = clientCommand.trim();
                    int length = clientCommand.length();
                    int first_n = clientCommand.indexOf(" ");
                    String s = clientCommand.substring(0, first_n);

                    num_passengers = Integer.parseInt(s);

                    coach_type = clientCommand.substring(length - 2);

                    doj_str = clientCommand.substring(length - 13, length - 3);
                    doj = LocalDate.parse(doj_str);

                    s = clientCommand.substring(first_n + 1, length - 14);
                    int last_n = s.lastIndexOf(" ");
                    String s2 = s.substring(last_n + 1);

                    train_id = Integer.parseInt(s2);

                    s = s.substring(0, last_n);

                    StringTokenizer st = new StringTokenizer(s, ",");

                    passenger_names = new String[num_passengers];
                    for (int i = 0; i < num_passengers; i++) {
                        String name = st.nextToken().trim();
                        passenger_names[i] = name;
                    }
                } catch (Exception e) {
                    responseQuery = "Invalid Input";
                    isCorrect = false;
                }
                if (!coach_type.equals("SL") && !coach_type.equals("AC")) {
                    responseQuery = "Wrong coach type. Available values: AC and SL.";
                    isCorrect = false;
                }

                if (isCorrect) {
                    try {
                        String query = "{? = call update_availability(?,?,?,?)}";
                        CallableStatement cstmt = Server.dbConnection.prepareCall(query);
                        cstmt.registerOutParameter(1, Types.INTEGER);
                        cstmt.setInt(2, train_id);
                        cstmt.setObject(3, doj);
                        cstmt.setString(4, coach_type);
                        cstmt.setInt(5, num_passengers);
                        cstmt.execute();
                        int avl_seat = cstmt.getInt(1);
                        cstmt.close();

                        if (avl_seat != -1) {
                            Statement stmt = Server.dbConnection.createStatement();
                            int total_seats;
                            if (coach_type.equals("SL")) {
                                ResultSet rs = stmt
                                        .executeQuery("SELECT number_sl_coaches FROM journey j WHERE j.train_id = "
                                                + train_id + " AND j.doj = '" + doj_str + "'::date");
                                rs.next();
                                total_seats = rs.getInt(1) * berths_per_sl;
                            } else {
                                ResultSet rs = stmt
                                        .executeQuery("SELECT number_ac_coaches FROM journey j WHERE j.train_id = "
                                                + train_id + " AND j.doj = '" + doj_str + "'::date");
                                rs.next();
                                total_seats = rs.getInt(1) * berths_per_ac;
                            }
                            int start_seat_num = total_seats - avl_seat - num_passengers;
                            // System.out.println(start_seat_num);
                            stmt.close();

                            Integer[] passenger_ids = new Integer[num_passengers];
                            for (int i = 0; i < num_passengers; i++) {
                                String add_passenger = "{? = call add_passenger_info(?)}";
                                cstmt = Server.dbConnection.prepareCall(add_passenger);
                                cstmt.registerOutParameter(1, Types.INTEGER);
                                cstmt.setString(2, passenger_names[i]);
                                cstmt.execute();
                                passenger_ids[i] = cstmt.getInt(1);
                                cstmt.close();
                            }
                            String book_ticket = "{? = call book_ticket(?,?,?,?,?,?)}";
                            cstmt = Server.dbConnection.prepareCall(book_ticket);
                            cstmt.registerOutParameter(1, Types.INTEGER);
                            cstmt.setInt(2, train_id);
                            cstmt.setObject(3, doj);
                            cstmt.setInt(4, num_passengers);
                            cstmt.setString(5, coach_type);

                            Array passenger_list = Server.dbConnection.createArrayOf("INTEGER", passenger_ids);
                            cstmt.setArray(6, passenger_list);
                            cstmt.setInt(7, start_seat_num);

                            cstmt.execute();
                            int pnr_num = cstmt.getInt(1);
                            cstmt.close();

                            responseQuery = "Ticket booked PNR: " + pnr_num + " Train Number: " + train_id + " Date: " + doj_str +"\n";

                            for (int i = 0; i < num_passengers; i++) {
                                query = "SELECT b.coach_no, b.berth_no FROM booked b WHERE b.passenger_id = ? AND b.pnr = ?";
                                PreparedStatement pstmt = Server.dbConnection.prepareStatement(query);
                                pstmt.setInt(1, passenger_ids[i]);
                                pstmt.setInt(2, pnr_num);
                                ResultSet rs = pstmt.executeQuery();
                                rs.next();
                                int coach_no = rs.getInt("coach_no");
                                int berth_no = rs.getInt("berth_no");
                                responseQuery += "\tName: "+passenger_names[i] + " Coach Number: " + coach_no + " Berth Number: " + berth_no + "\n";
                            }
                        } else {
                            query = "SELECT * FROM journey j WHERE j.train_id = ? AND j.doj = ?";
                            PreparedStatement stmt = Server.dbConnection.prepareStatement(query);
                            stmt.setInt(1, train_id);
                            stmt.setObject(2, doj);
                            ResultSet rs = stmt.executeQuery();
                            if (!rs.next()) {
                                responseQuery = "Train " + train_id + " on date " +doj_str+" not available.\n";
                            }
                            else{
                                responseQuery = "No seats available for train " + train_id + " on date " +doj_str+".\n";
                            }
                        }
                    } catch (SQLException e) {
                        Server.print_exception(e);
                    }
                }

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
    static int numServerCores = 8;

    // ------------ Start Server ----------------------
    public static void start_server() throws IOException {
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            // Creating a server socket to listen for clients
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
