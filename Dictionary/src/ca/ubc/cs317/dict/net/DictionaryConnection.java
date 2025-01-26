package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import static ca.ubc.cs317.dict.net.DictStringParser.splitAtoms;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;
    private Socket dictSocket;
    private BufferedReader in;
    private PrintWriter out;

    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        try {
            dictSocket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
            out = new PrintWriter(dictSocket.getOutputStream(), true);

            String welcomeMessage = in.readLine();

            if (welcomeMessage == null || !welcomeMessage.startsWith("220")){
                throw new DictConnectionException("Invalid welcome message");
            }
            System.out.println("CONNECTED TO SOCKET");
        } catch (UnknownHostException e) {
            throw new DictConnectionException("Unknown host: " + host, e);
        } catch (ConnectException e){
            throw new DictConnectionException("Server refused to connect: " + host, e);
        } catch (IOException e) {
            throw new DictConnectionException("I/O error during connection to " + host + ":" + port, e);
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {
        try {
            out.println("QUIT");
            out.flush();
            String reply = in.readLine();
            if (reply != null) {
                System.out.println("Server reply to QUIT: " + reply);
            }
        } catch (Exception e) {
            System.err.println("Warning: Exception during close: " + e.getMessage());
        } finally {
            try{
                if (dictSocket != null) {
                    dictSocket.close();
                }
                System.out.println("SOCKET CONNECTION SUCCESSFULLY CLOSED");
            } catch (Exception e) {
                System.err.println("Warning: Exception during close: " + e.getMessage());
            }
            dictSocket = null;
        }
    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

        if (word.split(" ").length > 1){
            word = '"' + word + '"';
        }

        String query = "DEFINE " + database.getName() + " " + word;

        try {
            out.println(query);
            out.flush();

            String initialResponse = in.readLine();
            String[] responseInfo = splitAtoms(initialResponse);
            if (responseInfo[0].equals("552") || responseInfo[0].equals("550")) {
                return set;
            } else if (responseInfo[0].equals("150")) {
                System.out.println("THERE ARE: " + responseInfo[1] + " definitions" );
            } else {
                throw new DictConnectionException("Undesired Response: " + responseInfo[0]);
            }

            int numberOfDefinitions = Integer.parseInt(responseInfo[1]);
            for (int i = 0; i < numberOfDefinitions; i++) {
                String responseOne = in.readLine();
                String[] responseCode = splitAtoms(responseOne);
                if (!responseCode[0].equals("151")) {
                    throw new DictConnectionException("Invalid Response: Status code should equal 151");
                }
                Definition definition = new Definition(responseCode[1], responseCode[2]);

                String definitionText;
                while (true) {
                    definitionText = in.readLine();
                    if (definitionText.equals(".")) break;
                    definition.appendDefinition(definitionText);
                }
                set.add(definition);
            }

            // Check Completion Status:
            String completionResponse = in.readLine();
            String[] completionInfo = splitAtoms(completionResponse);
            if (!completionInfo[0].equals("250")){
                throw new DictConnectionException("Competition Status Code is Wrong");
            }
        } catch (Exception e) {
            throw new DictConnectionException("Error during getMatchList", e);
        }

        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

        if (word.split(" ").length > 1){
            word = '"' + word + '"';
        }

        String query = "MATCH " + database.getName() + " " + strategy.getName() + " " + word;
        try {
            out.println(query);
            out.flush();

            String initialResponse = in.readLine();
            String[] responseInfo = splitAtoms(initialResponse);
            if (responseInfo[0].equals("550") || responseInfo[0].equals("551") || responseInfo[0].equals("552")) {
                return set;
            } else if (responseInfo[0].equals("152")) {
                System.out.println("THERE ARE: " + responseInfo[1] + " Matches" );
            } else {
                throw new DictConnectionException("Unknown response: " + responseInfo[0]);
            }

            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(".")){
                    break;
                }
                String[] matchedWords = splitAtoms(response);
                set.add(matchedWords[1]);
            }

            // Check Completion Status:
            String completionResponse = in.readLine();
            String[] completionInfo = splitAtoms(completionResponse);
            if (!completionInfo[0].equals("250")){
                throw new DictConnectionException("Competition Status Code is Wrong");
            }
        } catch (Exception e) {
            throw new DictConnectionException("Error during getMatchList", e);
        }

        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();
        try {
            out.println("SHOW DATABASES");
            out.flush();

            String initialResponse = in.readLine();
            String[] responseInfo = splitAtoms(initialResponse);
            if (responseInfo[0].equals("554")) {
                return databaseMap;
            } else if (responseInfo[0].equals("110")) {
                System.out.println("THERE ARE: " + responseInfo[1] + " Databases" );
            } else {
                throw new DictConnectionException("Unknown response: " + responseInfo[0]);
            }

            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(".")){
                    break;
                }
                String[] databaseInfo = splitAtoms(response);
                Database dbItem = new Database(databaseInfo[0], databaseInfo[1]);
                databaseMap.put(dbItem.getName(), dbItem);
            }

            // Check Completion Status:
            String completionResponse = in.readLine();
            String[] completionInfo = splitAtoms(completionResponse);
            if (!completionInfo[0].equals("250")){
                throw new DictConnectionException("Competition Status Code is Wrong");
            }
        } catch (Exception e){
            throw new DictConnectionException("Error during show databases", e);
        }
        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();
        try {
            out.println("SHOW STRATEGIES");
            out.flush();

            String initialResponse = in.readLine();
            String[] responseInfo = splitAtoms(initialResponse);
            if (responseInfo[0].equals("555")) {
                return set;
            } else if (responseInfo[0].equals("111")) {
                System.out.println("THERE ARE: " + responseInfo[1] + " strategies" );
            } else {
                throw new DictConnectionException("Unknown response: " + responseInfo[0]);
            }

            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(".")){
                    break;
                }
                String[] strategyInfo = splitAtoms(response);
                MatchingStrategy strategyItem = new MatchingStrategy(strategyInfo[0], strategyInfo[1]);
                set.add(strategyItem);
            }

            // Check Completion Status:
            String completionResponse = in.readLine();
            String[] completionInfo = splitAtoms(completionResponse);
            if (!completionInfo[0].equals("250")){
                throw new DictConnectionException("Competition Status Code is Wrong");
            }
        } catch (Exception e){
            throw new DictConnectionException("Error during show strategies", e);
        }
        return set;
    }

    /** Requests and retrieves detailed information about the currently selected database.
     *
     * @return A string containing the information returned by the server in response to a "SHOW INFO <db>" command.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized String getDatabaseInfo(Database d) throws DictConnectionException {
        StringBuilder sb = new StringBuilder();
        String query = "SHOW INFO " + d.getName();

        try {
            out.println(query);
            out.flush();

            String initialResponse = in.readLine();
            String[] responseInfo = splitAtoms(initialResponse);
            if (responseInfo[0].equals("550")) {
                return sb.toString();
            } else if (!responseInfo[0].equals("112")) {
                throw new DictConnectionException("Invalid Status Code: " + responseInfo[0]);
            }

            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(".")){
                    break;
                }
                sb.append(response);
                sb.append("\n");
            }

            // Check Completion Status:
            String completionResponse = in.readLine();
            String[] completionInfo = splitAtoms(completionResponse);
            if (!completionInfo[0].equals("250")){
                throw new DictConnectionException("Competition Status Code is Wrong");
            }
        } catch (Exception e) {
            throw new DictConnectionException("Error during getDatabaseInfo", e);
        }

        return sb.toString();
    }
}
