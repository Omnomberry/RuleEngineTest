package teo.isgci.xml;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import teo.isgci.appl.ISGCIResolver;

/**
 * 
 * @author WikiTeam ISGCI
 * 
 *         This class is used to update images, explanations in the smallgraph
 *         table and to add fakefamilys to the smallgraphtable
 * 
 */
public class UpdateSmallGraphs {

    private SQLWriter m_sql;
    private Connection m_connection;
    private TreeMap<String, String> images;
    private TreeMap<String, String> expl;
    private TreeMap<String, Set<String>> fakefamilies;

    public UpdateSmallGraphs() {
        this.startdatabaseConnections();
        images = null;
        expl = null;
        fakefamilies = null;
    }

    /*
     * =========================IMAGES===========================
     */
    /***
     * Call with one parameter which is the path to the graphlinks.xml including
     * the file itself!! Main should only be used if one wants to add images
     * afterwards
     * 
     * @param args
     */
    public static void main(String[] args) {

        UpdateSmallGraphs update = new UpdateSmallGraphs();

        if (args.length == 0) {
            System.out
                    .println("Please provide Path to graphlinks.xml as Parameter");
            return;
        } else {
            System.out.println("Updating Images");
            update.updateImages(args[0]);
            System.out.println("Images updated");
        }
    }

    /***
     * Reads in Smallgraph-Image-Mappings from a given xml
     * 
     * @param path
     *            path to graphlinks.xml
     * @return TreeMap containing Smallgraph Image Mapping
     * @throws MalformedURLException
     */
    public TreeMap<String, String> readImages(String path)
            throws MalformedURLException {
        XMLParser xml;
        ImageReader handler = new ImageReader();

        Resolver loader = new ISGCIResolver("file:"
                + System.getProperty("user.dir") + "/");

        xml = new XMLParser(loader.openInputSource(path), handler,
                loader.getEntityResolver());
        xml.parse();

        return handler.getImageMap();
    }

    /***
     * Updates Images of Smallgraphs using provided xml
     * 
     * @param path
     *            path to XML containing smallgraph-image-mapping
     */
    public void updateImages(String path) {

        try {
            images = readImages(path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        ResultSet names = null;
        try {
            Statement getNames = m_connection.createStatement();
            names = getNames.executeQuery("SELECT sg_name from smallgraph");

            /*
             * Iterate over all Smallgraphs and check whether there is a
             * corresponding Image. If so update entry in the database.
             */
            while (names.next()) {
                String current = names.getString("sg_name");
                String currentImage = images.get(current);
                if (currentImage != null)
                    m_sql.executeSingleSmallGraphSQLUPDATE(true, current,
                            currentImage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * =======================EXPLANATIONS=====================
     */

    /***
     * Updates Explanations of Smallgraphs
     */
    public void updateExplanations(String path) {

        try {
            expl = readExplanations(path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        ResultSet names = null;
        try {
            Statement getNames = m_connection.createStatement();
            names = getNames.executeQuery("SELECT sg_name from smallgraph");

            /*
             * Iterate over all Smallgraphs and check whether there is a
             * corresponding Explanation. If so update entry in the database.
             */
            while (names.next()) {
                String current = names.getString("sg_name");
                String currentExplanation = expl.get(current);
                if (currentExplanation != null)
                    m_sql.executeSingleSmallGraphSQLUPDATE(false, current,
                            currentExplanation);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /***
     * Reads in Smallgraph-Explanation-Mappings from a given xml
     * 
     * @param path
     *            path to graphlinks.xml
     * @return TreeMap containing Smallgraph Image Mapping
     * @throws MalformedURLException
     */
    private TreeMap<String, String> readExplanations(String path)
            throws MalformedURLException {

        XMLParser xml;
        ExplanationFakeFamilyReader handler = new ExplanationFakeFamilyReader();

        Resolver loader = new ISGCIResolver("file:"
                + System.getProperty("user.dir") + "/");

        xml = new XMLParser(loader.openInputSource(path), handler,
                loader.getEntityResolver(),
                new NoteFilter(SmallGraphTags.EXPL));
        xml.parse();

        return handler.getExplMap();
    }

    /*
     * =====================INSERT FAKEFAMILIES==================
     */

    /**
     * Inserts FakeFamilies into Database. This method is needed since
     * Fakefamilies are not handled by deduction processes hence they won't be
     * written into the database
     * 
     * @param path
     *            path to the smallgraphs.xml
     */
    public void insertFakeFamilies(String path) {

        try {
            fakefamilies = readFakeFamilies(path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Statement statement;
        ResultSet maxID = null;
        int sgid = 0;
        int aliasid = 0;

        /**
         * Get maxId from smallgraph and aliases. TODO For cleaner code, use
         * autoincrement of mySQL
         */
        try {
            statement = m_connection.createStatement();
            maxID = statement
                    .executeQuery("SELECT max(sg_id) as max FROM smallgraph");
            while (maxID.next())
                sgid = maxID.getInt("max") + 1;
            maxID = statement
                    .executeQuery("SELECT max(alias_id) as max FROM ALIAS");
            while (maxID.next())
                aliasid = maxID.getInt("max") + 1;
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        /**
         * Write in FakeFamilies and their aliases Explanations get updated with
         * updateExplanations
         */
        for (Map.Entry<String, Set<String>> entry : fakefamilies.entrySet()) {
            try {
                m_sql.insertNewSmallgraph(entry.getKey(), sgid, null,
                        "fakefamily", null, null);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for (String s : entry.getValue()) {
                try {
                    m_sql.insertNewAlias(aliasid++, entry.getKey(), s);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            sgid++;
        }
    }

    /***
     * Reads in Fakefamilies from a given xml
     * 
     * @param path
     *            path to graphlinks.xml
     * @return TreeMap containing Fakefamilies and there aliases
     * @throws MalformedURLException
     */
    private TreeMap<String, Set<String>> readFakeFamilies(String path)
            throws MalformedURLException {

        XMLParser xml;
        ExplanationFakeFamilyReader handler = new ExplanationFakeFamilyReader();

        Resolver loader = new ISGCIResolver("file:"
                + System.getProperty("user.dir") + "/");

        xml = new XMLParser(loader.openInputSource(path), handler,
                loader.getEntityResolver(),
                new NoteFilter(SmallGraphTags.EXPL));
        xml.parse();

        return handler.getFakeMap();
    }

    /*
     * ================DATABASE CONNECTION================
     */

    /***
     * starts both databaseConnection to Reader and Writer Change
     * databaseAdress, AccountName and Password respectively
     */
    private void startdatabaseConnections() {
        String databaseAdress = "jdbc:mySQL://localhost/Spectre";
        String databaseAccountName = "root";
        String databaseAccountPassword = "";

        callReader(databaseAdress, databaseAccountName,
                databaseAccountPassword);

        callWriter(databaseAdress, databaseAccountName,
                databaseAccountPassword);
    }

    /***
     * creates connection to database for reading out databaseentrys
     * 
     * @param databaseAddress
     *            address of database
     * @param username
     *            username for database
     * @param password
     *            password for database
     */
    private void callReader(String databaseAddress, String username,
            String password) {
        String driver = "org.gjt.mm.mysql.Driver";

        try {
            Class.forName(driver);

            m_connection = DriverManager.getConnection(databaseAddress,
                    username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /***
     * Creates Database Connection to update database
     * 
     * @param databaseAdress
     *            address of database
     * @param databaseAccountName
     *            username for database
     * @param databaseAccountPassword
     *            password for database
     */
    private void callWriter(String databaseAdress, String databaseAccountName,
            String databaseAccountPassword) {

        try {
            if ((databaseAdress != null) && (databaseAccountName != null)
                    && (databaseAccountPassword != null)) {
                m_sql = new SQLWriter(databaseAdress, databaseAccountName,
                        databaseAccountPassword, "", false);
            }
        } catch (SQLException e) {
            System.out.println("Problems with sql");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Problems with property/complexity tables");
            e.printStackTrace();
        }
    }

}
