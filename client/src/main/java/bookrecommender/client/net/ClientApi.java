package bookrecommender.client.net;

public class ClientApi {

    private final ClientConnection connection;

    public ClientApi(ClientConnection connection) {
        this.connection = connection;
    }

    public String getBookById(int id) {
        connection.send("GET_BOOK " + id);
        try {
            return connection.receive();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String searchByTitle(String title) {
        connection.send("SEARCH_TITLE " + title);
        try {
            return connection.receive();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
}
