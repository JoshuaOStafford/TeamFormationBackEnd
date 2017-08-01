public class User {

    private int rank;
    String name;
    boolean matched;
    int[] preferences;

    public User(int rank_, String username, int[] preferences_){
        rank = rank_;
        name = username;
        matched = false;
        preferences = new int[preferences_.length];
        System.arraycopy(preferences_, 0, preferences, 0, preferences_.length);
    }

    int getBestChoiceDictator(User[] MasterList){
        for (int choice : preferences){
            if (!MasterList[choice].matched)
                return choice;
        }
        return -1;
    }

    int getBestChoice(User[] MasterList){
        int index = 0;
        int bestChoice = -1;
        boolean found = false;
        while (!found && (index != preferences.length)){
            int pick = preferences[index];

            // if users best choice is already matched or doesn't have them in their preference list at all, skip them
            if (MasterList[pick].matched || MasterList[pick].wontChoose(rank))
                ++index;
            else{
                found = true;
                bestChoice = pick;
            }
        }
        return bestChoice;
    }

    int getBestChoiceRequired(User[] MasterList){
        int bestChoice = getBestChoice(MasterList);
        if (bestChoice == -1){
            for (int index = 0; index<MasterList.length; ++index){
                if (!MasterList[index].matched && index != rank)
                    return index;
            }
            return -1;
        } else{
            return bestChoice;
        }
    }

    private boolean wontChoose(int proposer){
        for (int choice : preferences){
            if (choice == proposer)
                return false;   // the proposer is at least on the preference list
        }
        return true;    // the proposer has no chance of being chosen
    }

    double getAveragePreferenceRank(User[] ML, double theta){
        double total = 0.0;
        int users = 0;
        for (int index = 0; index<preferences.length; ++index) {
            if (!ML[index].matched) {
                total += index;
                users++;
            }
        }
        if (users==0)
            return theta;
        return total / users;
    }


    int getPreferenceRank(int proposer){
        for (int index = 0; index<preferences.length; ++index){
            if (proposer == preferences[index])
                return index;
        }
        return 2 * preferences.length;
    }

    int getRank(){
        return rank;
    }

}

