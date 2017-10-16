import java.util.LinkedList;
import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;


/**
 * This class runs various algorithms to form teams of users. The main method is driven by the setup Data offered by the
 * Python code that runs this class and all of the user data is also supplied by the Python code.
 *
 * This main class utilizes the User.java file which supplies an easy way to store all the important data about each
 * user and helper methods for the main team forming algorithms.
 *
 * This class also uses a Team.java file to store the formed teams.
 *
 * The output of this class is to the console, where the Python code that called this class processes each line of
 * output to form teams on the backend of the Team Formation platform.
 *
 *
 */
public class Main {

    public static void main(String[] args) {

        // Takes in arguments passed from Python code to supply the parameters, users, and preferences

        String setupData = args[0]; // This is a 4 integer array explained below as setup Array

        // each user is associated with their rank index in this data, with their preferences displayed after
        // Format: Jack 0 2 3 4 Jill ... -> meaning Jack is rank index 0 and his preferences are 2, 3, 4 then himself
        String preferenceData = args[1];

        // ARRAY WILL BE 4 integers: NumberOfUsers, GroupSize, True/False for running IS, Index of Algorithm
        int[] setupArray = getSetupArray(setupData);

        int numOfUsers = setupArray[0];
        int groupSize = setupArray[1];
        boolean runIS = (setupArray[2] == 1);
        String algorithm = getAlgorithmFromIndex(setupArray[3]);
        // here is parameter for the heuristic of RPM
        double lower=0.35;
        double upper=1.0-lower;

        // ML stands for Master List, where all the user's ranks & preferences can be found with other helper methods
        User[] ML = new User[numOfUsers];

        // fills User MasterList with the preference Data from python
        getUsers(ML, preferenceData);

        ArrayList<Team> teams = new ArrayList<>();

        // CMA is Current Match Array, and it is used in the Sub-game Perfect Equilibrium algorithm
        int[] CMA = new int[numOfUsers];
        for (int index = 0; index < CMA.length; ++index)
            CMA[index] = index; // initialized so that all users start as singletons


        // We Run Iterative Soulmates as a preliminary algorithm if it was selected on the site
        if (runIS)
            iterativeSoulmates(ML, teams, groupSize);

        // Next we run the specified algorithm
        if (algorithm.equals("RandomSerialDictatorship"))
            randomSerialDictatorship(ML, teams, groupSize);
        else if (algorithm.equals("Heuristic"))
            heuristicApproach(ML, teams, groupSize, 0, 200);
        else if (algorithm.equals("RotationalProposerMechanism"))
            runRotationalProposerMechanism(ML, teams, lower, upper);
        printTeams(ML, teams);
    }


    /**
     * Method sets up the inputs to the AAM_SM_Heuristic class and runs the Rotational Proposer Mechanism
     * Sets up the data to run Jian's algorithm
     * @param ML - array of Users
     * @param teams - ArrayList of all the teams (empty when passed in, filled when method finishes)
     * @param lower - lower threshold to accept
     * @param upper - upper threshold to accept
     */
    private static void runRotationalProposerMechanism(User[] ML, ArrayList<Team> teams, double lower, double upper){
        int n = ML.length; // n is number of players
        int depth = ML.length; // set depth to number of players
        LinkedList<Integer> order = new LinkedList<Integer>(); // linked list of the order in which players propose
        for (User player : ML)
            order.add(player.getRank());

        // double ArrayList where value.get(i).get(j) is the utility of i if they are paired with j
        ArrayList<ArrayList<Integer>> value = new ArrayList<ArrayList<Integer>>();

        // ArrayList containing an LinkedList for each player, the LinkedList describes the ordering of the players preferences
        ArrayList<LinkedList<Integer>> linked_value = new ArrayList<LinkedList<Integer>>();

        // Offset for switch between zero-based indexing and one-based indexing
        value.add(0, new ArrayList<Integer>());
        linked_value.add(0, new LinkedList<Integer>());

        // set-up linked_value and value Double Arrays
        for(User player : ML) {

            int rawUtility = player.preferences.length;
            LinkedList<Integer> preferencesList = new LinkedList<Integer>();
            ArrayList<Integer> utilityList = new ArrayList<Integer>(ML.length);
            utilityList.add(0); // offset additional zero for switch to one-based indexing
            for(User index : ML)
                utilityList.add(0);

            for (int index = 0; index < player.preferences.length; ++index) {
                int desired_teammate = player.preferences[index]+1;
                // add desired teammate to current players linked list of preferences
                preferencesList.add(desired_teammate);

                // store the utility of the current player if they match with current desired teammate
                utilityList.set(desired_teammate, rawUtility);
                --rawUtility; // linearly decrease the raw utility after each preference}
            }
            linked_value.add(player.getRank()+1, preferencesList); // add preference list for player in
            value.add(player.getRank()+1, utilityList);
        }

        // Run Jian's algorithm
        AAM_SN_Heuristic TeamFormationHeuristic = new AAM_SN_Heuristic(n, depth, order, value, linked_value, lower, upper);

        // the value at each index of the array is the teammate who the index player matched with. If the value is 0,
        // the index player is a Singleton
        ArrayList<Integer> array = TeamFormationHeuristic.ARM();

        // form teams based off the algorithm
        for (int index = 1; index < array.size(); ++index){
            if (array.get(index) != 0 && !ML[index-1].matched){
                int[] teammates = new int[2];
                int currentPlayerUtility = array.get(index);
                int partner = value.get(index).indexOf(currentPlayerUtility) - 1; // -1 to account for shift back to zero-based indexing
                teammates[0] = index-1; // -1 to account for shift back to zero-based indexing
                teammates[1] = partner;
                formTeams(ML, teammates, teams);
            }
        }

    }


    /**
     * Iterative Soulmates: this is a pre-processing algorithm that will form any teams of the specified size that meet
     *                      the Soulmates Criteria
     * Soulmates Criteria: for a team of n, the team passes Soulmates Criteria if each member of the team ranks all
     *                     other members of the team within their n-1 preferred partners
     *
     * @param ML - Array of user objects
     * @param teams - Array list to store teams
     * @param groupSize - size that the groups must be (this number is n in Soulmates Criteria)
     */
    private static void iterativeSoulmates(User[] ML, ArrayList<Team> teams, int groupSize) {
        boolean matchFound;
        do {
            matchFound = false;
            for (int proposer = 0; (proposer < ML.length); ++proposer) {

                boolean teamFailedToForm = false;
                if (!ML[proposer].matched) {

                    // method returns array of size zero if proposer couldn't fill team with remaining preferences.
                    int[] potential_members = getProposersIdealRemainingTeam(ML, proposer, groupSize);
                    boolean teamIsFilled = potential_members.length != 0;

                    // If the proposer had enough preferences to potentially form a team, we check if that team meets
                    // the Soulmates criteria
                    if (teamIsFilled){
                        // determine if all members of the team like each other
                        for (int member = 0; !teamFailedToForm && member < potential_members.length; ++member){
                            for (int potential_teammate = 0; !teamFailedToForm && potential_teammate < potential_members.length; ++potential_teammate){
                                if (potential_members[member] != potential_members[potential_teammate]){
                                    if (!ML[potential_members[member]].meetsSoulmateCriteria(potential_members[potential_teammate], groupSize, ML)) {
                                        teamFailedToForm = true;
                                    }
                                }
                            }
                        }
                    } else {
                        teamFailedToForm = true;
                    }

                    // If the team did not fail to form, a team of soulmates were found!
                    // Team is stored and we iterate again through all users to attempt to find another soulmates team
                    if (!teamFailedToForm){
                        teams = formTeams(ML, potential_members, teams);
                        matchFound = true;
                        proposer = ML.length; // start back at the beginning and search again for soulmates
                    }

                }
            }

        } while (matchFound); // iterate again and keep looking for soulmates
    }


    /**
     * Heuristic Approach to forming teams. The users take turns proposing teams and the offer is accepted by user X if
     * the average preference rank of the other users on the team is better than the average preference ranking of all
     * of user X's remaining preferences.
     * @param ML - Array of Users
     * @param teams - Array list to store teams
     * @param groupSize - size of the groups
     * @param theta - floating number that changes the likeliness of a user to accept or reject the team. IF theta is
     *              positive, the user is more likely to accept an offer. If theta is negative, they are less likely.
     * @param maxRounds - the number of rounds of offers that are made. This is the way we stop the algorithm from being
     *               an infinite loop
     */
    private static void heuristicApproach(User[] ML, ArrayList<Team> teams, int groupSize, double theta, int maxRounds){
        int currentRound = 0;
        while (!allUsersMatched(ML) && currentRound < maxRounds){
            boolean finished = false;
            ++currentRound;
            for (int proposer = 0; proposer < ML.length && !finished; ++proposer){
                if (!ML[proposer].matched){
                    int[] potential_members = getProposersIdealRemainingTeam(ML, proposer, groupSize);
                    if (potential_members.length != 0 && teamWantsToForm(ML, potential_members, theta)){
                        teams = formTeams(ML, potential_members, teams);
                        finished = allUsersMatched(ML);
                        proposer = ML.length;
                    }
                }
            }
        }
    }


    private static boolean teamWantsToForm(User[] ML, int[] potential_members, double theta){

        // proposer is 0 so no need to check if they want to be on team
        for (int member = 1; member < potential_members.length; ++member) {
            double remainingAvgRank = ML[member].getAveragePreferenceRank(ML, theta);
            double groupAvgRank = ML[member].proposedTeamsRankAverage(potential_members);

            // 
            boolean didntLikeTeam = remainingAvgRank + theta < groupAvgRank;
            if (didntLikeTeam)
                return false;
        }
        return true;
    }


    /**
     * Algorithm forms teams based on letting Dictators (the User with the best ranking) to pick their whole team with
     * no input from any of the other users. Algorithm runs until all users have been matched or until each user has had
     * the option to be the dictator.
     * @param ML - Array of users
     * @param teams - ArrayList to store teams
     * @param groupSize - how many people the dictator can put on his team (himself included)
     */
    private static void randomSerialDictatorship(User[] ML, ArrayList<Team> teams, int groupSize) {
        for (int dictator = 0; dictator < ML.length && !allUsersMatched(ML); ++dictator) {
            if (!ML[dictator].matched) {
                if (ML[dictator].getBestChoiceDictator(ML) != -1) {
                    Team dictatorsTeam = new Team(teams.size() + 1);
                    dictatorsTeam.addMember(ML[dictator]);
                    ML[dictator].matched = true;
                    for (int index = 0; index < groupSize - 1; ++index) {
                        int choice = ML[dictator].getBestChoiceDictator(ML);
                        if (choice != -1) {
                            dictatorsTeam.addMember(ML[choice]);
                            ML[choice].matched = true;
                        }
                    }
                    teams.add(dictatorsTeam);
                }
            }
        }
    }


    private static ArrayList<Team> formTeams(User[] ML, int[] teammates, ArrayList<Team> teams){
        Team newTeam = new Team(teams.size() + 1);
        for (int member : teammates){
            newTeam.addMember(ML[member]);
            ML[member].matched = true;
        }
        teams.add(newTeam);
        return teams;
    }


    private static int[] getProposersIdealRemainingTeam(User[] ML, int proposer, int groupSize){
        boolean teamIsFilled = false;
        int[] potential_members = new int[groupSize];
        potential_members[0] = proposer;
        int currGroupSize = 1;
        for (int index = 0; index < ML[proposer].preferences.length && !teamIsFilled; ++index) {
            int choice = ML[proposer].preferences[index];
            if (!ML[choice].matched && choice != ML[proposer].getRank()) {
                potential_members[currGroupSize] = choice;
                ++currGroupSize;
            }
            if (currGroupSize == groupSize) {
                teamIsFilled = true;
            }
        }
        if (teamIsFilled)
            return potential_members;
        else
            return new int[0];
    }


    /**
     * Helper method that returns true if all users have a match and false if not
     * @param ML - array of Users
     * @return - boolean signalling if all users are matched
     */
    private static boolean allUsersMatched(User[] ML) {
        for (User user : ML) {
            if (!user.matched)
                return false;
        }
        return true;
    }


    /**
     * Method sets up the important information that the main driver needs to execute the correct algorithms
     * @param args - the setup data from Python code
     * @return - an array that specifies number of users, size of teams, whether to run Iterative Soulmates or not
     * (1 if run, 0 if not), and the index of the algorithm to run
     */
    private static int[] getSetupArray(String args){
        Scanner lineProcessor = new Scanner(args);
        int[] setupArray = {-1, -1, -1, -1};
        setupArray[0] = lineProcessor.nextInt();
        setupArray[1] = lineProcessor.nextInt();
        setupArray[2] = lineProcessor.nextInt();
        setupArray[3] = lineProcessor.nextInt();
        lineProcessor.close();
        return setupArray;
    }


    /**
     * Method reads in the preferenceData from the Python code and sets up the User MasterList
     * @param ML - basically a global array to access the users
     * @param preferences - string data from Python code
     */
    private static void getUsers(User[] ML, String preferences){
        Scanner prefScanner = new Scanner(preferences);
        for (int currUser = 0; currUser<ML.length; ++currUser){
            String username = prefScanner.next();
            int rank = prefScanner.nextInt();
            String alone_or_team = prefScanner.next();
            int[] prefArrayRaw = new int[ML.length];
            Arrays.fill(prefArrayRaw, -1);
            int index = 0;
            while (prefScanner.hasNextInt()) {
                prefArrayRaw[index] = prefScanner.nextInt();
                ++index;
            }
            int count = 0;
            while (count != ML.length && prefArrayRaw[count] != -1) {
                count++;
            }
            int[] prefArray = new int[count];
            System.arraycopy(prefArrayRaw, 0, prefArray, 0, prefArray.length);
            boolean prefers_alone = alone_or_team.equals("alone");
            ML[rank] = new User(rank, username, prefArray, prefers_alone);
        }

    }


    /**
     * Method prints out the teams to the console in a very specific way that allows the python code that runs this
     * Java code to put the appropriate teams into the database
     * @param ML - The global user array
     * @param teams - All of the teams that have been formed
     */
    private static void printTeams(User[] ML, ArrayList<Team> teams) {
        for (Team team : teams) {
            System.out.println("T: " + team.membersToString(ML));
        }
        System.out.print("S: ");
        for (User user : ML) {
            if (!user.matched)
                System.out.print(user.name + " ");
        }
    }


    /**
     * Method takes the index of which algorithm to run from Python code and gets the proper name for readability
     * @param index - from python code
     * @return - String name of algorithm to run
     */
    private static String getAlgorithmFromIndex(int index){
        if (index == 0)
            return "RandomSerialDictatorship";
        if (index == 1)
            return "Heuristic";
        if (index == 2)
            return "RotationalProposerMechanism";
        return "none";
    }

}
