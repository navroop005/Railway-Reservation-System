import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Scanner;

import org.postgresql.util.PSQLException;

public class Server {

   private static Connection dbConnection;

   public static void main(String[] args) {
      dbConnection = open_database();
      initialize_database();
      add_trains_data("./inputs/train_input.txt");
      try {
         ServiceModule.start_server();
      } catch (Exception e) {
         print_exception(e);
      }
   }

   public static Connection open_database() {
      Connection c = null;
      try {
         Class.forName("org.postgresql.Driver");
         Properties properties = new Properties();
         properties.setProperty("user", "postgres");
         properties.setProperty("password", "12345");
         properties.setProperty("escapeSyntaxCallMode", "callIfNoReturn");

         c = DriverManager
               .getConnection("jdbc:postgresql://localhost:5432/railway_reservation_system", properties);

      } catch (Exception e) {
         print_exception(e);
      }
      System.out.println("Opened database successfully");
      return c;
   }

   public static void initialize_database() {
      try {
         File schemaFile = new File("./sql/schema.pgsql");
         Scanner sc = new Scanner(schemaFile);
         String query = "";
         while (sc.hasNextLine()) {
            query += sc.nextLine() + " ";
         }
         Statement stmt = dbConnection.createStatement();
         stmt.executeUpdate(query);
         stmt.close();
         sc.close();

         File functionFile = new File("./sql/functions.pgsql");
         sc = new Scanner(functionFile);
         query = "";
         while (sc.hasNextLine()) {
            query += sc.nextLine() + " ";
         }
         stmt = dbConnection.createStatement();
         stmt.executeUpdate(query);
         stmt.close();
         sc.close();

         try {
            stmt = dbConnection.createStatement();
            query = "INSERT INTO coach_info VALUES('berths_per_ac', 18); " +
                  "INSERT INTO coach_info VALUES('berths_per_sl', 24);";
            stmt.executeUpdate(query);
            stmt.close();
         } catch (PSQLException e) {
            if (!e.getSQLState().equals("23505"))  {
               print_exception(e);
            }
         }
      } catch (Exception e) {
         print_exception(e);
      }
   }

   public static void add_train(int train_id, String doj, int num_ac, int num_sl) {
      String query = "{CALL add_journey_info(?, ?, ?, ?)}";
      try {
         CallableStatement stmt = dbConnection.prepareCall(query);
         stmt.setInt(1, train_id);
         stmt.setObject(2, LocalDate.parse(doj));
         stmt.setInt(3, num_ac);
         stmt.setInt(4, num_sl);
         // System.out.println(stmt);
         stmt.execute();
         stmt.close();
         System.out.println("Train id: " + train_id + ", DOJ: " + doj + " added!");
      } catch (SQLException e) {
         String sqlState = e.getSQLState();
         if (sqlState.equals("23505")) {
            System.out.println("Train id: " + train_id + ", DOJ: " + doj + " already exists!");
         } else {
            print_exception(e);
         }
      }
   }

   public static void add_trains_data(String inp_file) {
      try {
         File inp = new File(inp_file);
         Scanner sc = new Scanner(inp);
         while (sc.hasNextLine()) {
            String journey = sc.nextLine();
            String[] s = journey.split(" ");
            int train_id = Integer.parseInt(s[0]);
            String doj = s[1];
            int num_ac = Integer.parseInt(s[2]);
            int num_sl = Integer.parseInt(s[3]);

            add_train(train_id, doj, num_ac, num_sl);
         }
         sc.close();
      } catch (Exception e) {
         print_exception(e);
      }
   }

   public static void print_exception(Exception e){
      e.printStackTrace();
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
   }
}