package pkg.vms;

import pkg.vms.DAO.ClientsDAO;
import pkg.vms.model.Clients;

public class Main {
    public static void main(String[] args) {

        ClientsDAO dao = new ClientsDAO();

        try {
            // INSERT
            Clients c = new Clients(0, "Clarence", "clarence@mail.com", "Port Louis", "57912345");
            dao.insert(c);
            System.out.println("Client inserted!");

            // GET ALL
            System.out.println("\n-- ALL CLIENTS --");
            for (Clients cl : dao.getAll()) {
                System.out.println(cl.getRef_client() + " | " + cl.getNom_client());
            }

            // GET BY ID
            Clients client = dao.getById(1);
            if (client != null) {
                System.out.println("\nClient 1 = " + client.getNom_client());
            }

            // UPDATE
            client.setNom_client("Clarence Updated");
            dao.update(client);
            System.out.println("Client updated!");

            // DELETE
            dao.delete(1);
            System.out.println("Client deleted!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
