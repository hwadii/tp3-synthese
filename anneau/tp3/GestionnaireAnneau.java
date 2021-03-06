package anneau.tp3;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class GestionnaireAnneau extends UnicastRemoteObject implements GestionnaireInterface {

    private static final long serialVersionUID = -273169080821700334L;
    private int id;
    private ArrayList<Integer> liste = new ArrayList<Integer>();
    private int idrelai;

    public GestionnaireAnneau(int val) throws RemoteException {
        id = val;
        idrelai=-1;
    }

    // Service appelé par un nouveau site pour s'ajouter à la liste et actualise les
    // services suivants des sites
    // num: id du nouveau site
    public synchronized void ajoutSite(int num) throws MalformedURLException, RemoteException, NotBoundException {
        if (liste.size()>0){
            try {
                if (liste.contains(num)) liste.remove(liste.indexOf(num));
                SiteInterface sitePrecedent = (SiteInterface) Naming.lookup("rmi://localhost/Site"+id+(liste.get(liste.size()-1)));
                sitePrecedent.getSuivant(num);
                try {
                    SiteInterface siteDernier = (SiteInterface) Naming.lookup("rmi://localhost/Site"+id+num);
                    siteDernier.getSuivant(liste.get(0));
                    SiteInterface sitePremier = (SiteInterface) Naming.lookup("rmi://localhost/Site"+id + liste.get(0));
                    sitePremier.exist();
                    liste.add(num);
                } catch (RemoteException e) {
                    
                    System.out.println("Panne du suivant " + liste.get(0));
                    panne(liste.get(0));
                    ajoutSite(num);
                }
            } catch (RemoteException e) {

                System.out.println("Panne du précédent " + liste.get(liste.size()-1));
                panne(liste.get(liste.size()-1));
                ajoutSite(num);
            }
        } else {
            liste.add(num);
        }
    }

    //Service appelé par le site détectant la panne pour supprimer le site de la liste et actualise les services suivants des sites
    //num: id du site en panne
    public synchronized void panne(int num) throws RemoteException, MalformedURLException, NotBoundException {
        int indexCourant = liste.indexOf(num);
        if (indexCourant!=0){
            SiteInterface sitePrecedent = (SiteInterface) Naming.lookup("rmi://localhost/Site"+id+(indexCourant-1));
            if (indexCourant != liste.size()-1){
                try{
                    sitePrecedent.getSuivant(liste.get(indexCourant+1));
                } catch(RemoteException e){
                    panne(indexCourant-1);
                }
            } else if (liste.size() > 2) {
                try{
                    sitePrecedent.getSuivant(liste.get(0));
                } catch (RemoteException e){
                    panne(indexCourant-1);
                }
            }
        } else if (liste.size() > 2) {
            SiteInterface sitePrecedent = (SiteInterface) Naming.lookup("rmi://localhost/Site"+id+(liste.size()-1)) ;
            try {
                sitePrecedent.getSuivant(liste.get(1));
            } catch (RemoteException e) {
                panne(liste.size()-1);
            }
        }
        liste.remove(liste.indexOf(num));
    }

    public int getIdRelai() throws RemoteException{
        return idrelai;
    }

    public void setIdRelai(int id) throws RemoteException{
        idrelai = id;
    }

    //On crée un gestionnaire d'anneau et on expose ses services
    public static void main(String[] args) throws NotBoundException, RemoteException, MalformedURLException {
        GestionnaireAnneau serveurAnneau = new GestionnaireAnneau(Integer.parseInt(args[0]));
        ServeurInterface serveurCentral = (ServeurInterface) Naming.lookup("rmi://localhost/ServeurCentral");
        serveurCentral.ajoutSousReseau(Integer.parseInt(args[0]));
        Naming.rebind ("SousReseau"+args[0], serveurAnneau);
    }

}
