import java.util.ArrayList;

class Team {

    private ArrayList<User> members;
    int number;

    Team(int number_){
        members = new ArrayList<User>();
        number = number_;
    }

    void addMember(User user){
        members.add(user);
    }

    StringBuilder membersToString(User[] ML){
        StringBuilder teamString = new StringBuilder();
        for (User user : members){
            teamString.append(user.name + " ");
        }
        return teamString;
    }

}
