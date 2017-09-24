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
            iterativeSoulmatesExtended(ML, teams, groupSize);

        // Next we run the specified algorithm
        if (algorithm.equals("RandomSerialDictatorship"))
            randomSerialDictatorship(ML, teams, groupSize);
        else if (algorithm.equals("Heuristic"))
            heuristicApproach(ML, teams, groupSize, 0, 200);
        else if (algorithm.equals("Sub-game Perfect Equilibrium"))
            subGamePerfectEquilibrium(ML, teams, CMA);
        printTeams(ML, teams);
    }


    /**
     * Iterative Soulmates: this is a pre-processing algorithm that will form any teams of two in which the pair are
     * the other's number one choice. If a user's first number one choice is already paired with their soul-mate, then
     * their next choice will become their potential soul-mate
     * @param ML - Array of user objects
     * @param teams - Array list to store teams
     */
    private static void iterativeSoulmates(User[] ML, ArrayList<Team> teams) {
        boolean match = false;
        do {
            for (int proposer = 0; (proposer < ML.length); ++proposer) {
                if (proposer == ML.length - 1)
                    match = false;
                if (!ML[proposer].matched) {
                    int topChoice = ML[proposer].getBestChoice(ML);
                    if (topChoice != -1 && proposer == ML[topChoice].getBestChoice(ML)) {
                        match = true;
                        Team newTeam = new Team(teams.size() + 1);
                        newTeam.addMember(ML[proposer]);
                        newTeam.addMember(ML[topChoice]);
                        teams.add(newTeam);
                        ML[proposer].matched = true;
                        ML[topChoice].matched = true;
                        proposer = ML.length;   // makes the for loop start back at beginning after match is found
                    }
                }
            }
        } while (match);
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
    private static void iterativeSoulmatesExtended(User[] ML, ArrayList<Team> teams, int groupSize) {
        boolean matchFound;
        do {
            matchFound = false;
            for (int proposer = 0; (proposer < ML.length); ++proposer) {

                boolean teamFailedToForm = false;
                if (!ML[proposer].matched) {

                    int[] potential_members = new int[groupSize]; // store all the members of the
                    int groupIndex = 1;
                    potential_members[0] = proposer;

                    boolean teamIsFilled = false;
                    for (int index = 0; index < ML[proposer].preferences.length && !teamIsFilled; ++index) {
                        int choice = ML[proposer].preferences[index];
                        if (!ML[choice].matched && choice != ML[proposer].getRank()) {
                            potential_members[groupIndex] = choice;
                            ++groupIndex;
                        }
                        if (groupIndex == groupSize) {
                            teamIsFilled = true;
                        }
                    }

                    // If the proposer had enough preferences to potentially form a team, we check if that team meets
                    // the Soulmates criteria
                    if (teamIsFilled){
                        // determine if all members of the team like each other
                        for (int member = 0; !teamFailedToForm && member < potential_members.length; ++member){
                            for (int potential_teammate = 0; !teamFailedToForm && potential_teammate < potential_members.length; ++potential_teammate){
                                if (potential_members[member] != potential_members[potential_teammate]){
                                    if (!ML[potential_members[member]].meetsSoulmateCriteria(potential_members[potential_teammate], groupSize, ML)) { // CHANGE THIS LINE TO CALL METHOD TO CHECK SOULMATES CRITERIA
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
                        Team newTeam = new Team(teams.size() + 1);
                        for (int members_index : potential_members) {
                            newTeam.addMember(ML[members_index]);
                            ML[members_index].matched = true;
                        }
                        teams.add(newTeam);
                        matchFound = true;
                        proposer = ML.length; // start back at the beginning and search again for soulmates
                    }

                }
            }

        } while (matchFound); // iterate again and keep looking for soulmates
    }


    /**
     * Sub Game Perfect Equilibrium - The teams that would be formed if everyone knew everything
     * @param ML - Master List of Users
     * @param teams - currently formed teams
     * @param CMA - Current Match Array
     */
    private static void subGamePerfectEquilibrium(User[] ML, ArrayList<Team> teams, int[] CMA){
        int x = ML.length - 1;
        boolean[] iterativeSoulmatesMatches = new boolean[ML.length];
        for (int index = 0; index < ML.length; ++index)
            iterativeSoulmatesMatches[index] = ML[index].matched;
        while (x >= 0){
            for (int index = 0; index < ML.length; ++index)
                ML[index].matched = iterativeSoulmatesMatches[index];
            CMA = findTeamsIfXGoesFirst(ML, CMA, x);
            --x;
        }
        teams = formTeamsForSGPE(ML, teams, CMA);
    }


    /**
     * Finds the optimal teams for each sub-game of the overall game. Forms the teams if each user before X was rejected
     * by all that they offered to.
     * @param ML - Array of Users
     * @param CMA - represents each user's current best option
     * @param starter - user that goes first in the sub-game because all others before them were rejected
     * @return - New array of users' current best options
     */
    private static int[] findTeamsIfXGoesFirst(User[] ML, int[] CMA, int starter){
        for (int currentUser = starter; currentUser < ML.length; ++currentUser) {
            User proposer = ML[currentUser];
            boolean satisfied = false;
            for (int index = 0; index < proposer.preferences.length && !satisfied; ++index) {
                int choice = proposer.preferences[index];
                int proposerRank = ML[choice].getPreferenceRank(currentUser);
                if (((proposerRank < ML[choice].getPreferenceRank(CMA[choice])) ||
                        canStealShouldSettle(ML, CMA, currentUser, choice)) && !ML[choice].matched) {
                    // update CMA
                    int oldMatchOfUser = CMA[currentUser];
                    int oldMatchOfChoice = CMA[choice];
                    CMA[oldMatchOfChoice] = oldMatchOfChoice;
                    CMA[oldMatchOfUser] = oldMatchOfUser;
                    CMA[choice] = currentUser;
                    CMA[currentUser] = choice;
                    ML[currentUser].matched = true;
                    ML[choice].matched = true;
                    satisfied = true;
                } else if (proposerRank == ML[choice].getPreferenceRank(CMA[choice]))
                    satisfied = true;
            }
        }
        return CMA;
    }


    /**
     * can steal, should settle - special helper method to detect a potential situation where a user's utility will
     * be lower in the long run if they do not accept an offer that at the moment seems worse than their current state
     * @param ML - array of users
     * @param CMA - current best matches of users
     * @param proposer - the user proposing who seems worse than the choice's current best option
     * @param choice - the choice who needs to decide whether to accept or reject the proposer
     * @return - the decision whether to accept or reject the offer based on if the proposer will later steal the
     *           choice's current match if the choice rejects
     */
    private static boolean canStealShouldSettle(User[] ML, int[] CMA, int proposer, int choice){
        int currentChoice = ML[proposer].getPreferenceRank(choice);
        boolean accept = false;
        for (int choiceCount = currentChoice + 1; choiceCount < ML[proposer].preferences.length; ++choiceCount){
            int newChoice = ML[proposer].preferences[choiceCount];
            if (ML[newChoice].getPreferenceRank(CMA[newChoice]) >= ML[newChoice].getPreferenceRank(proposer))
                accept = true;
            if (accept && newChoice == CMA[choice]){
                return true;
            } else if (accept && newChoice != CMA[choice])
                return false;
        }
        return false;
    }


    /**
     * Helper method to form teams after the SGPE algorithm is run by using the CMA list
     * @param ML - array of user objects
     * @param teams - the teams stored in an array list
     * @param CMA - current match array
     * @return - the teams
     */
    private static ArrayList<Team> formTeamsForSGPE(User[] ML, ArrayList<Team> teams, int[] CMA){
        for (int currentUser = 0; currentUser < CMA.length; ++currentUser){
            if (CMA[currentUser] != -1){
                int choice = CMA[currentUser];
                if (currentUser != choice) {
                    Team newTeam = new Team(teams.size() + 1);
                    newTeam.addMember(ML[currentUser]);
                    newTeam.addMember(ML[choice]);
                    CMA[choice] = -1;
                    ML[currentUser].matched = true;
                    ML[choice].matched = true;
                    teams.add(newTeam);
                }
            }
        }
        return teams;
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
     * @param rounds - the number of rounds of offers that are made. This is the way we stop the algorithm from being
     *               an infinite loop
     */
    private static void heuristicApproach(User[] ML, ArrayList<Team> teams, int groupSize, double theta, int rounds) {
        int count = 0;
        while (!allUsersMatched(ML) && count < rounds) {
            boolean finished = false;
            for (int proposer = 0; proposer < ML.length && !finished; ++proposer) {
                int choice = ML[proposer].getBestChoiceRequired(ML);
                if (choice != -1 && !ML[proposer].matched) {
                    double leftoverRankAverage = ML[choice].getAveragePreferenceRank(ML, theta);
                    int proposerRank = ML[choice].getPreferenceRank(proposer);
                    boolean accept = leftoverRankAverage > proposerRank - theta;
                    if (accept) {
                        Team newTeam = new Team(teams.size() + 1);
                        newTeam.addMember(ML[proposer]);
                        newTeam.addMember(ML[choice]);
                        teams.add(newTeam);
                        ML[proposer].matched = true;
                        ML[choice].matched = true;
                        if (allUsersMatched(ML))
                            finished = true;
                    }
                }
            }
            ++count;
        }

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
            prefArrayRaw[index] = rank;
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
            return "Sub-game Perfect Equilibrium";
        return "none";
    }

}
