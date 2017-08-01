import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        String setupData = args[0];
        String preferenceData = args[1];

        // ARRAY WILL BE NumberOfUsers, GroupSize, True/False for running IS, Index of Algorithm
        int[] setupArray = getSetupArray(setupData);

        int numOfUsers = setupArray[0];
        int groupSize = setupArray[1];
        boolean runIS = (setupArray[2] == 1);
        String algorithm = getAlgorithmFromIndex(setupArray[3]);
        User[] ML = new User[numOfUsers];
        getUsers(ML, preferenceData);
        ArrayList<Team> teams = new ArrayList<>();
        int[] CMA = new int[numOfUsers];
        for (int index = 0; index < CMA.length; ++index)
            CMA[index] = index;
        if (runIS)
            iterativeSoulmates(ML, teams);
        if (algorithm.equals("RandomSerialDictatorship"))
            randomSerialDictatorship(ML, teams, groupSize);
        else if (algorithm.equals("AlgorithmX"))
            algorithmX(ML, teams, groupSize, 0, 20);
        else if (algorithm.equals("Sub-game Perfect Equilibrium"))
            subGamePerfectEquilibrium(ML, teams, CMA);
        printTeams(ML, teams);
    }




    /* ALGORITHMS THAT HAVE BEEN IMPLEMENTED */

    // Iterative Soulmates
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


    // Sub Game Perfect Equilibrium
    private static void subGamePerfectEquilibrium(User[] ML, ArrayList<Team> teams, int[] CMA){
        int x = ML.length - 1;
        while (x >= 0){
            for (User user : ML)
                user.matched = false;
            CMA = findTeamsIfXGoesFirst(ML, CMA, x);
            --x;
        }

        // Form teams with CMA List
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

    }

    // find teams if X proposes first
    private static int[] findTeamsIfXGoesFirst(User[] ML, int[] CMA, int starter){
        for (int currentUser = starter; currentUser < ML.length; ++currentUser) {
            User proposer = ML[currentUser];
            boolean satisfied = false;
            for (int index = 0; index < proposer.preferences.length && !satisfied; ++index) {
                int choice = proposer.preferences[index];
                int proposerRank = ML[choice].getPreferenceRank(currentUser);
                if (((proposerRank < ML[choice].getPreferenceRank(CMA[choice])) || canStealShouldSettle(ML, CMA, currentUser, choice)) && !ML[choice].matched) {
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


    // can steal, should settle
    private static boolean canStealShouldSettle(User[] ML, int[] CMA, int proposer, int choice){
        int currentChoice = ML[proposer].getPreferenceRank(choice);
        int match = -1;
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






    // Algorithm X
    private static void algorithmX(User[] ML, ArrayList<Team> teams, int groupSize, double theta, int rounds) {
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


    // Random Serial Dictatorship
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


    /* HELPER FUNCTIONS FOR THE ALGORITHMS */
    private static boolean allUsersMatched(User[] ML) {
        for (User user : ML) {
            if (!user.matched)
                return false;
        }
        return true;
    }


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

    private static void getUsers(User[] ML, String preferences){
        Scanner prefScanner = new Scanner(preferences);
        for (int currUser = 0; currUser<ML.length; ++currUser){
            String username = prefScanner.next();
            int rank = prefScanner.nextInt();
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
            ML[rank] = new User(rank, username, prefArray);
        }
    }


    /* FUNCTION TO PRINT OUT THE RESULTS */
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

    private static String getAlgorithmFromIndex(int index){
        if (index == 0)
            return "RandomSerialDictatorship";
        if (index == 1)
            return "AlgorithmX";
        if (index == 2)
            return "Sub-game Perfect Equilibrium";
        return "none";
    }


}