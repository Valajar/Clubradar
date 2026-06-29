package Login.forms;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class ClubDatabase {
    private File eingetrageneClubs = new File("C/Users/milo/IdeaProjects/Clubradar/swingRadar/src/Login/forms/Clubs.txt");
    private static ClubDatabase clubdatabase;
    private static ArrayList<clubinfos> clubs = new ArrayList<>();

    public ClubDatabase(){
        try (Scanner myReader = new Scanner(eingetrageneClubs)) {
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                System.out.println(data);
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }



}

